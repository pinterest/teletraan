import argparse
import os
from random import randrange
import time
import traceback
import daemon
import logging

from deployd.client.client import Client
from deployd.common.config import Config
from deployd.common.exceptions import AgentException
from deployd.common.helper import Helper
from deployd.common.single_instance import SingleInstance
from deployd.common.env_status import EnvStatus
from deployd.common import utils
from deployd.common.executor import Executor
from deployd.common.types import DeployReport, PingStatus, DeployStatus, OpCode, \
    DeployStage, AgentStatus
from deployd import IS_PINTEREST

log = logging.getLogger(__name__)


class PingServer(object):
    def __init__(self, ag):
        self._agent = ag

    def __call__(self, deploy_report):
        return self._agent.update_deploy_status(deploy_report=deploy_report)


class DeployAgent(object):

    _STATUS_FILE = None
    _curr_report = None
    _config = None
    _env_status = None

    def __init__(self, client, estatus=None, conf=None, executor=None, helper=None):
        self._response = None
        # a map maintains env_name -> deploy_status
        self._envs = {}
        self._config = conf or Config()
        self._executor = executor
        self._helper = helper or Helper(self._config)
        self._STATUS_FILE = self._config.get_env_status_fn()
        self._client = client
        self._env_status = estatus or EnvStatus(self._STATUS_FILE)
        # load environment deploy status file from local disk
        self.load_status_file()

    def load_status_file(self):
        self._envs = self._env_status.load_envs()
        if not self._envs:
            self._envs = {}
            self._curr_report = None
            return

        self._curr_report = self._envs.values()[0]
        self._config.update_variables(self._curr_report)

    def serve_build(self):
        """This is the main function of the ``DeployAgent``.
        """
        log.info('The deploy agent is starting.')
        if not self._executor:
            self._executor = Executor(callback=PingServer(self), config=self._config)

        # start to ping server to get the latest deploy goal
        response = self._client.send_reports(self._envs)
        self._response = response
        if self._response:
            report = self._update_internal_deploy_goal(self._response)
            # failed to update
            if report.status_code != AgentStatus.SUCCEEDED:
                self._update_ping_reports(deploy_report=report)
                self._client.send_reports(self._envs)
                return

        while self._response and self._response.opCode != OpCode.NOOP:
            try:
                # update the current deploy goal
                if self._response.deployGoal:
                    deploy_report = self.process_deploy(self._response)
                else:
                    log.info('No new deploy goal to get updated')
                    deploy_report = DeployReport(AgentStatus.SUCCEEDED)

                if deploy_report.status_code == AgentStatus.ABORTED_BY_SERVER:
                    log.info('switch to the new deploy goal: {}'.format(self._response.deployGoal))
                    continue

            except Exception:
                # anything catch-up here should be treated as agent failure
                deploy_report = DeployReport(status_code=AgentStatus.AGENT_FAILED,
                                             error_code=1,
                                             output_msg=traceback.format_exc(),
                                             retry_times=1)

            self.update_deploy_status(deploy_report)
            if deploy_report.status_code in [AgentStatus.AGENT_FAILED,
                                             AgentStatus.TOO_MANY_RETRY,
                                             AgentStatus.SCRIPT_TIMEOUT]:
                log.error('Unexpeted exceptions: {}, error message {}'.format(
                    deploy_report.status_code, deploy_report.output_msg))
                return

        self.clean_stale_builds()
        if self._response and self._response.deployGoal:
            self._update_internal_deploy_goal(self._response)

        if self._response:
            log.info('Complete the current deploy with response: {}.'.format(self._response))
        else:
            log.info('Failed to get response from server, exit.')

    def serve_forever(self):
        log.info("Running deploy agent in daemon mode")
        while True:
            try:
                self.serve_build()
                time.sleep(self._config.get_daemon_sleep_time())
            except:
                log.exception("Deploy Agent got exception: {}".format(traceback.format_exc()))

    def serve_once(self):
        log.info("Running deploy agent in non daemon mode")
        try:
            # randomly sleep some time before pinging server
            sleep_secs = randrange(10)
            log.info("Randomly sleep {} seconds before starting.".format(sleep_secs))
            time.sleep(sleep_secs)
            self.serve_build()
        except Exception:
            log.exception("Deploy Agent got exceptions: {}".format(traceback.format_exc()))

    def serve(self, run_as_daemon=False):
        if run_as_daemon:
            with daemon.DaemonContext():
                self.serve_forever()
        else:
            self.serve_once()

    def process_deploy(self, response):
        op_code = response.opCode
        deploy_goal = response.deployGoal
        if op_code == OpCode.TERMINATE or op_code == OpCode.DELETE:
            if deploy_goal.envName in self._envs:
                del self._envs[deploy_goal.envName]
            else:
                log.info('Cannot find env {} in the ping report'.format(deploy_goal.env_name))

            if self._curr_report.report.envName == deploy_goal.envName:
                self._curr_report = None

            return DeployReport(AgentStatus.SUCCEEDED, retry_times=1)
        else:
            curr_stage = deploy_goal.deployStage
            '''
            DOWNLOADING and STAGING are two reserved deploy stages owned by agent:
            DOWNLOADING: download the tarball from pinrepo
            STAGING: In this step, deploy agent will chmod and change the symlink pointing to
              new service code, and etc.
            '''
            if curr_stage == DeployStage.DOWNLOADING:
                return self._executor.run_cmd(self.get_download_script(deploy_goal=deploy_goal))
            elif curr_stage == DeployStage.STAGING:
                log.info("set up symbolink for the package: {}".format(deploy_goal.deployId))
                return self._executor.run_cmd(self.get_staging_script())
            else:
                return self._executor.execute_command(curr_stage)

    # provides command line to start download scripts or tar ball.
    def get_download_script(self, deploy_goal):
        if not (deploy_goal.build and deploy_goal.build.artifactUrl):
            raise AgentException('Cannot find build or build url in the deploy goal')

        url = deploy_goal.build.artifactUrl
        build = deploy_goal.build.buildId
        env_name = self._curr_report.report.envName
        return ['deploy-downloader', '-f', self._config.get_config_filename(),
                '-v', build,  '-u', url, "-e", env_name]

    def get_staging_script(self):
        build = self._curr_report.build_info.build_id
        env_name = self._curr_report.report.envName
        return ['deploy-stager', '-f', self._config.get_config_filename(),
                '-v', build,  '-t', self._config.get_target(), "-e", env_name]

    def _update_ping_reports(self, deploy_report):
        if self._curr_report:
            self._curr_report.update_by_deploy_report(deploy_report)

        """
        if we failed to dump the status to the disk. We should notify the server
        as agent failure. We set the current report to be agent failure, so server would
        tell agent to abort current deploy, then exit
        """
        result = self._env_status.dump_envs(self._envs)
        if (not result) and self._curr_report:
            self._curr_report.update_by_deploy_report(
                DeployReport(status_code=AgentStatus.AGENT_FAILED,
                             error_code=1,
                             output_msg='Failed to dump status to the disk'))

    def update_deploy_status(self, deploy_report):
        self._update_ping_reports(deploy_report=deploy_report)
        response = self._client.send_reports(self._envs)
        """
        if we failed to get any response from server, set the self._response to None
        """
        if response is None:
            log.info('Failed to get response from server')
            self._response = None
            return PingStatus.PING_FAILED
        else:
            plan_changed = DeployAgent.plan_changed(self._response, response)
            self._response = response
            report = self._update_internal_deploy_goal(self._response)
            if report.status_code != AgentStatus.SUCCEEDED:
                self._update_ping_reports(report)
                self._response = self._client.send_reports(self._envs)
                return PingStatus.PLAN_CHANGED

            if plan_changed:
                return PingStatus.PLAN_CHANGED
            else:
                return PingStatus.PLAN_NO_CHANGE

    def clean_stale_builds(self):
        if not self._envs:
            return

        if not (self._curr_report and self._curr_report.report):
            return

        builds_to_keep = [status.build_info.build_id for status in self._envs.values()
                          if status.build_info]
        builds_dir = self._config.get_builds_directory()
        num_retain_builds = self._config.get_num_builds_retain()
        build_name = self._curr_report.report.envName
        # clear stale builds
        if len(builds_to_keep) > 0:
            self.clean_stale_files(build_name, builds_dir, builds_to_keep, num_retain_builds)

    def clean_stale_files(self, build_name, dir, files_to_keep, num_file_to_retain):
        for build in self._helper.get_stale_builds(self._helper.builds_available_locally(dir),
                                                   num_file_to_retain):
            if build not in files_to_keep:
                log.info("Stale file {} found in {}... removing.".format(
                    build, dir))
                self._helper.clean_package(dir, build, build_name)

    # private functions: update per deploy step configuration specified by services owner on the
    # environment config page
    def _update_internal_deploy_goal(self, response):
        deploy_goal = response.deployGoal
        if not deploy_goal:
            log.info('No deploy goal to be updated.')
            return DeployReport(status_code=AgentStatus.SUCCEEDED)

        # use envName as status map key
        env_name = deploy_goal.envName
        if (self._envs is None) or (self._envs.get(env_name) is None):
            self._envs[env_name] = DeployStatus()

        # update deploy_status from response for the environemnt
        self._envs[env_name].update_by_response(response)

        # update script varibales
        if deploy_goal.scriptVariables:
            log.info('Start to generate script variables for deploy: {}'.
                     format(deploy_goal.deployId))
            env_dir = self._config.get_agent_directory()
            working_dir = os.path.join(env_dir, "{}_SCRIPT_CONFIG".format(env_name))
            with open(working_dir, "w+") as f:
                for key, value in deploy_goal.scriptVariables.items():
                    f.write("{}={}\n".format(key, value))

        # load deploy goal to the config
        self._curr_report = self._envs[env_name]
        self._config.update_variables(self._curr_report)
        self._executor.update_configs(self._config)
        log.info('current deploy goal is: {}'.format(deploy_goal))
        return DeployReport(status_code=AgentStatus.SUCCEEDED)

    def _update_deploy_alias(self, deploy_goal):
        env_name = deploy_goal.envName
        if not self._envs or (env_name not in self._envs):
            log.warning('Env name does not exist, ignore it.')
        elif deploy_goal.deployAlias:
            self._envs[env_name].deployAlias = deploy_goal.deployAlias
            log.warning('Update deploy alias to {} for {}'.format(deploy_goal.deployAlias,
                                                                  deploy_goal.envName))

    @staticmethod
    def plan_changed(old_response, new_response):
        if not old_response:
            return new_response

        # if the opcode has changed
        if old_response.opCode != new_response.opCode:
            return True

        if not old_response.deployGoal:
            return new_response.deployGoal

        if not new_response.deployGoal:
            return old_response.deployGoal

        # if this a new deploy
        if old_response.deployGoal.deployId != new_response.deployGoal.deployId:
            return True

        # if this is a new deploy stage
        if old_response.deployGoal.deployStage != new_response.deployGoal.deployStage:
            return True

        return False

# make sure only one instance is running
instance = SingleInstance()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('-e', '--server_stage', dest='stage', default='prod',
                        help="This option is deprecated")
    parser.add_argument('-f', '--config-file', dest='config_file', required=False, default='deployd/conf/agent.conf',
                        help="the deploy agent config file path.")
    parser.add_argument('-d', '--daemon', dest="daemon", action='store_true')
    parser.add_argument('-n', '--host_name', dest="hostname", required=False, default=None)
    parser.add_argument('-g', '--group', dest='hostgroup', required=False, default=None)
    args = parser.parse_args()
    config = Config(args.config_file)
    utils.run_prereqs(config)

    if IS_PINTEREST:
        import pinlogger
        pinlogger.initialize_logger(logger_filename='deploy-agent.log')
        pinlogger.LOG_TO_STDERR = True
    else:
        log_filename = os.path.join(config.get_log_directory(), 'deploy-agent.log')
        logging.basicConfig(filename=log_filename, level=config.get_log_level())
        
    log.info("Start to run deploy-agent.")
    client = Client(config=config, hostname=args.hostname, hostgroup=args.hostgroup)
    agent = DeployAgent(client=client, conf=config)
    utils.listen()
    agent.serve(args.daemon)

if __name__ == '__main__':
    main()
