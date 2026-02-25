# Copyright 2016 Pinterest, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
from typing import List, Optional, Union
import logging
import os
import sys
from random import randrange
import socket
import time
import traceback

from deployd.client.client import Client
from deployd.common.config import Config
from deployd.common.exceptions import AgentException
from deployd.common.helper import Helper
from deployd.common.env_status import EnvStatus
from deployd.common.single_instance import SingleInstance
from deployd.common.stats import TimeElapsed, create_sc_timing, create_sc_increment
from deployd.common.utils import (
    get_telefig_version,
    get_container_health_info,
    check_prereqs,
)
from deployd.common.utils import uptime as utils_uptime, listen as utils_listen
from deployd.common.executor import Executor
from deployd.common.types import (
    DeployReport,
    PingStatus,
    DeployStatus,
    OpCode,
    DeployStage,
    AgentStatus,
)
from deployd import __version__, IS_PINTEREST, MAIN_LOGGER
from deployd.types.deploy_goal import DeployGoal
from deployd.types.ping_response import PingResponse

DEPLOY_INFO_METRIC_NAME = "deploy.info"
DEPLOY_INFO_METRIC_VALUE = 1

log: logging.Logger = logging.getLogger(name=MAIN_LOGGER)


class PingServer(object):
    def __init__(self, ag) -> None:
        self._agent = ag

    def __call__(self, deploy_report) -> int:
        return self._agent.update_deploy_status(deploy_report=deploy_report)


class DeployAgent(object):
    _STATUS_FILE = None
    _curr_report = None
    _config = None
    _env_status = None

    def __init__(
        self, client, estatus=None, conf=None, executor=None, helper=None
    ) -> None:
        self._response = None
        # a map maintains env_name -> deploy_status
        self._envs = {}
        self._config = conf or Config()
        self._executor = executor
        self.stat_time_elapsed_internal = TimeElapsed()
        self.stat_time_elapsed_total = TimeElapsed()
        self.stat_stage_time_elapsed = None
        self.deploy_goal_previous = None
        self._first_run = False
        self._helper = helper or Helper(self._config)
        self._STATUS_FILE = self._config.get_env_status_fn()
        self._client = client
        self._env_status = estatus or EnvStatus(self._STATUS_FILE)
        # load environment deploy status file from local disk
        self.load_status_file()
        self._telefig_version = get_telefig_version()

    def load_status_file(self) -> None:
        self._envs = self._env_status.load_envs()
        if not self._envs:
            self._envs = {}
            self._curr_report = None
            return

        self._curr_report = list(self._envs.values())[0]
        self._config.update_variables(self._curr_report)

    @property
    def first_run(self) -> bool:
        """check if this the very first run of agent on this instance.
        first_run will evaluate to True, even if self._envs is set, until the process has exited.
        self._envs is not populated when running for the first time on a new instance
        return: bool self._first_run
        """
        if self._first_run or not self._envs:
            self._first_run = True
        return self._first_run

    def _send_deploy_status_stats(self, deploy_report) -> None:
        if not self._response.deployGoal or not deploy_report:
            return

        tags = {"first_run": self.first_run}
        if self._response.deployGoal.deployStage:
            tags["deploy_stage"] = self._response.deployGoal.deployStage
        if self._response.deployGoal.envName:
            tags["env_name"] = self._response.deployGoal.envName
        if self._response.deployGoal.stageName:
            tags["stage_name"] = self._response.deployGoal.stageName
        if deploy_report.status_code:
            tags["status_code"] = deploy_report.status_code
        if self._telefig_version:
            tags["telefig_version"] = self._telefig_version
        create_sc_increment("deployd.stats.deploy.status.sum", tags=tags)

    def serve_build(self) -> None:
        """This is the main function of the ``DeployAgent``."""

        log.info("The deploy agent is starting.")
        if not self._executor:
            self._executor = Executor(callback=PingServer(self), config=self._config)
        # include healthStatus info for each container
        if len(self._envs) > 0:
            for status in self._envs.values():
                # for each service, we check the container health status
                log.info(f"the current service is: {status.report.envName}")
                try:
                    if status.report.redeploy is None:
                        status.report.redeploy = 0
                    healthStatus = get_container_health_info(
                        status.build_info.build_commit,
                        status.report.envName,
                        status.report.redeploy,
                    )
                    if (
                        healthStatus
                        and status.report.deployStage != DeployStage.STOPPED
                        and status.report.deployStage != DeployStage.STOPPING
                    ):
                        if "redeploy" in healthStatus:
                            status.report.redeploy = int(healthStatus.split("-")[1])
                            status.report.wait = 0
                            status.report.state = "RESET_BY_SYSTEM"
                            status.report.containerHealthStatus = None
                        else:
                            status.report.containerHealthStatus = healthStatus
                            status.report.state = None
                            if "unhealthy" not in healthStatus:
                                if status.report.wait is None or status.report.wait > 5:
                                    status.report.redeploy = 0
                                    status.report.wait = 0
                                else:
                                    status.report.wait = status.report.wait + 1
                    else:
                        status.report.state = None
                        status.report.containerHealthStatus = None
                except Exception:
                    status.report.state = None
                    status.report.containerHealthStatus = None
                    log.exception(
                        "get exception while trying to check container health: {}".format(
                            traceback.format_exc()
                        )
                    )
                    continue
            self._env_status.dump_envs(self._envs)
        # start to ping server to get the latest deploy goal
        self._response = self._client.send_reports(self._envs)
        # we only need to send RESET once in one deploy-agent run
        if len(self._envs) > 0:
            for status in self._envs.values():
                if status.report.state == "RESET_BY_SYSTEM":
                    status.report.state = None
            self._env_status.dump_envs(self._envs)

        if self._response:
            report = self._update_internal_deploy_goal(self._response)
            # failed to update
            if report.status_code != AgentStatus.SUCCEEDED:
                self._update_ping_reports(deploy_report=report)
                self._client.send_reports(self._envs)
                return

        while (
            self._response
            and self._response.opCode
            and self._response.opCode != OpCode.NOOP
        ):
            try:
                # ensure stat_time_elapsed_internal is always running
                self.stat_time_elapsed_internal.resume()
                # update the current deploy goal
                if self._response.deployGoal:
                    deploy_report = self.process_deploy(self._response)
                    # resume stat_time_elapsed_internal immediately, previous process_deploy may have paused
                    self.stat_time_elapsed_internal.resume()
                else:
                    log.info("No new deploy goal to get updated")
                    deploy_report = DeployReport(AgentStatus.SUCCEEDED)

                if deploy_report.status_code == AgentStatus.ABORTED_BY_SERVER:
                    log.info(
                        "switch to the new deploy goal: {}".format(
                            self._response.deployGoal
                        )
                    )
                    continue

            except Exception:
                # anything catch-up here should be treated as agent failure
                # resume stat_time_elapsed_internal, previous process_deploy may have paused
                self.stat_time_elapsed_internal.resume()
                deploy_report = DeployReport(
                    status_code=AgentStatus.AGENT_FAILED,
                    error_code=1,
                    output_msg=traceback.format_exc(),
                    retry_times=1,
                )

            self._send_deploy_status_stats(deploy_report)

            if PingStatus.PING_FAILED == self.update_deploy_status(deploy_report):
                return

            if deploy_report.status_code in [
                AgentStatus.AGENT_FAILED,
                AgentStatus.TOO_MANY_RETRY,
                AgentStatus.SCRIPT_TIMEOUT,
            ]:
                log.error(
                    "Unexpected exceptions: {}, error message {}".format(
                        deploy_report.status_code, deploy_report.output_msg
                    )
                )
                return

        self.clean_stale_builds()
        if self._response and self._response.deployGoal:
            self._update_internal_deploy_goal(self._response)

        if self._response:
            log.info(
                "Complete the current deploy with response: {}.".format(self._response)
            )
        else:
            log.info("Failed to get response from server, exit.")

        try:
            self._send_deploy_info_metrics()
        except Exception as e:
            log.error(f"failed to send deploy info metrics: {e}")

    def serve_forever(self) -> None:
        log.info("Running deploy agent in daemon mode")
        while True:
            try:
                self.serve_build()
            except Exception:
                log.exception(
                    "Deploy Agent got exception: {}".format(traceback.format_exc())
                )
            finally:
                time.sleep(self._config.get_daemon_sleep_time())
                self.load_status_file()

    def serve_once(self) -> None:
        log.info("Running deploy agent in non daemon mode")
        try:
            if len(self._envs) > 0:
                # randomly sleep some time before pinging server
                # TODO: consider pause stat_time_elapsed_internal here
                sleep_secs = randrange(self._config.get_init_sleep_time())
                log.info(
                    "Randomly sleep {} seconds before starting.".format(sleep_secs)
                )
                time.sleep(sleep_secs)
            else:
                log.info("No status file. Could be first time agent ran")
            self.serve_build()
        except Exception:
            log.exception(
                "Deploy Agent got exceptions: {}".format(traceback.format_exc())
            )

    def _resolve_deleted_env_name(self, envName, envId) -> Optional[str]:
        # When server return DELETE goal, the envName might be empty if the env has already been
        # deleted. This function would try to figure out the envName based on the envId in the
        # DELETE goal.
        if envName:
            return envName
        for name, value in self._envs.items():
            if envId == value.report.envId:
                return name
        return None

    def process_deploy(self, response) -> DeployReport:
        self.stat_time_elapsed_internal.resume()
        op_code = response.opCode
        deploy_goal = response.deployGoal
        if op_code == OpCode.TERMINATE or op_code == OpCode.DELETE:
            envName = self._resolve_deleted_env_name(
                deploy_goal.envName, deploy_goal.envId
            )
            if envName in self._envs:
                del self._envs[envName]
            else:
                log.info("Cannot find env {} in the ping report".format(envName))

            if self._curr_report.report.envName == deploy_goal.envName:
                self._curr_report = None

            return DeployReport(AgentStatus.SUCCEEDED, retry_times=1)
        else:
            curr_stage = deploy_goal.deployStage
            """
            DOWNLOADING and STAGING are two reserved deploy stages owned by agent:
            DOWNLOADING: download the tarball from pinrepo
            STAGING: In this step, deploy agent will chmod and change the symlink pointing to
              new service code, and etc.
            """
            # pause stat_time_elapsed_internal so that external actions are not counted
            self.stat_time_elapsed_internal.pause()
            log.info(f"The current deploy stage is: {curr_stage}")
            if curr_stage == DeployStage.DOWNLOADING:
                return self._executor.run_cmd(
                    self.get_download_script(deploy_goal=deploy_goal)
                )
            elif curr_stage == DeployStage.STAGING:
                log.info(
                    "set up symbolink for the package: {}".format(deploy_goal.deployId)
                )
                return self._executor.run_cmd(self.get_staging_script())
            else:
                return self._executor.execute_command(curr_stage)

    # provides command line to start download scripts or tar ball.
    def get_download_script(self, deploy_goal) -> List[str]:
        if not (deploy_goal.build and deploy_goal.build.artifactUrl):
            raise AgentException("Cannot find build or build url in the deploy goal")

        url = deploy_goal.build.artifactUrl
        build = deploy_goal.build.buildId
        env_name = self._curr_report.report.envName
        if not self._config.get_config_filename():
            return ["deploy-downloader", "-v", build, "-u", url, "-e", env_name]
        else:
            return [
                "deploy-downloader",
                "-f",
                self._config.get_config_filename(),
                "-v",
                build,
                "-u",
                url,
                "-e",
                env_name,
            ]

    def get_staging_script(self) -> list:
        build = self._curr_report.build_info.build_id
        env_name = self._curr_report.report.envName
        if not self._config.get_config_filename():
            return [
                "deploy-stager",
                "-v",
                build,
                "-t",
                self._config.get_target(),
                "-e",
                env_name,
            ]
        else:
            return [
                "deploy-stager",
                "-f",
                self._config.get_config_filename(),
                "-v",
                build,
                "-t",
                self._config.get_target(),
                "-e",
                env_name,
            ]

    def _update_ping_reports(self, deploy_report) -> None:
        if self._curr_report:
            self._curr_report.update_by_deploy_report(deploy_report)

        # if we failed to dump the status to the disk. We should notify the server
        # as agent failure. We set the current report to be agent failure, so server would
        # tell agent to abort current deploy, then exit
        result = self._env_status.dump_envs(self._envs)
        if (not result) and self._curr_report:
            self._curr_report.update_by_deploy_report(
                DeployReport(
                    status_code=AgentStatus.AGENT_FAILED,
                    error_code=1,
                    output_msg="Failed to dump status to the disk",
                )
            )

    def update_deploy_status(self, deploy_report) -> int:
        self._update_ping_reports(deploy_report=deploy_report)
        response = self._client.send_reports(self._envs)

        # if we failed to get any response from server, return failure but don't reset previous response
        if response is None:
            log.info("Failed to get response from server")
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

    def clean_stale_builds(self) -> None:
        if not self._envs:
            return

        if not (self._curr_report and self._curr_report.report):
            return

        builds_to_keep = [
            status.build_info.build_id
            for status in self._envs.values()
            if status.build_info
        ]
        builds_dir = self._config.get_builds_directory()
        num_retain_builds = self._config.get_num_builds_retain()
        env_name = self._curr_report.report.envName
        # clear stale builds
        if len(builds_to_keep) > 0:
            self.clean_stale_files(
                env_name, builds_dir, builds_to_keep, num_retain_builds
            )

    def clean_stale_files(
        self, env_name, dir, files_to_keep, num_file_to_retain
    ) -> None:
        for build in self._helper.get_stale_builds(
            self._helper.builds_available_locally(dir, env_name), num_file_to_retain
        ):
            if build not in files_to_keep:
                log.info("Stale file {} found in {}... removing.".format(build, dir))
                self._helper.clean_package(dir, build, env_name)

    def _timing_stats_deploy_stage_time_elapsed(self) -> None:
        """a deploy goal has finished, send stats for the elapsed time"""
        if (
            self.deploy_goal_previous
            and self.deploy_goal_previous.deployStage
            and self.stat_stage_time_elapsed
        ):
            tags = {"first_run": self.first_run}
            if self.deploy_goal_previous.deployStage:
                tags["deploy_stage"] = self.deploy_goal_previous.deployStage
            if self.deploy_goal_previous.envName:
                tags["env_name"] = self.deploy_goal_previous.envName
            if self.deploy_goal_previous.stageName:
                tags["stage_name"] = self.deploy_goal_previous.stageName
            create_sc_timing(
                "deployd.stats.deploy.stage.time_elapsed_sec",
                self.stat_stage_time_elapsed.get(),
                tags=tags,
            )

    # private functions: update per deploy step configuration specified by services owner on the
    # environment config page
    def _update_internal_deploy_goal(self, response) -> DeployReport:
        deploy_goal = response.deployGoal
        if not deploy_goal:
            log.info("No deploy goal to be updated.")
            self._timing_stats_deploy_stage_time_elapsed()
            return DeployReport(status_code=AgentStatus.SUCCEEDED)

        # use envName as status map key
        env_name = deploy_goal.envName
        if (self._envs is None) or (self._envs.get(env_name) is None):
            self._envs[env_name] = DeployStatus()

        # update deploy_status from response for the environment
        self._envs[env_name].update_by_response(response)

        # update script variables
        env_dir = self._config.get_agent_directory()
        script_config_path = os.path.join(env_dir, f"{env_name}_SCRIPT_CONFIG")
        if deploy_goal.scriptVariables:
            log.info(
                "Start to generate script variables for deploy: {}".format(
                    deploy_goal.deployId
                )
            )
            with open(script_config_path, "w+") as f:
                for key, value in deploy_goal.scriptVariables.items():
                    f.write(f"{key}={value}\n")
        else:
            # Remove the script config file if scriptVariables is None or empty
            if os.path.exists(script_config_path):
                try:
                    os.remove(script_config_path)
                    log.info(
                        f"Removed script config file: {script_config_path} because scriptVariables is empty or None."
                    )
                except Exception as e:
                    log.warning(
                        f"Failed to remove script config file {script_config_path}: {e}"
                    )

        # timing stats - deploy stage start
        if deploy_goal != self.deploy_goal_previous:
            # a deploy goal has changed
            tags = {"first_run": self.first_run}

            # deploy stage has changed, close old previous timer
            self._timing_stats_deploy_stage_time_elapsed()

            # create a new timer for the new deploy goal
            if deploy_goal.deployStage:
                tags["deploy_stage"] = deploy_goal.deployStage
            if deploy_goal.envName:
                tags["env_name"] = deploy_goal.envName
            if deploy_goal.stageName:
                tags["stage_name"] = deploy_goal.stageName
            self.stat_stage_time_elapsed = TimeElapsed()
            create_sc_timing(
                "deployd.stats.deploy.stage.time_start_sec",
                self.stat_stage_time_elapsed.get(),
                tags=tags,
            )
            self.deploy_goal_previous = deploy_goal

        # load deploy goal to the config
        self._curr_report = self._envs[env_name]
        self._config.update_variables(self._curr_report)
        self._executor.update_configs(self._config)
        log.info("current deploy goal is: {}".format(deploy_goal))
        return DeployReport(status_code=AgentStatus.SUCCEEDED)

    def _update_deploy_alias(self, deploy_goal) -> None:
        env_name = deploy_goal.envName
        if not self._envs or (env_name not in self._envs):
            log.warning("Env name does not exist, ignore it.")
        elif deploy_goal.deployAlias:
            self._envs[env_name].deployAlias = deploy_goal.deployAlias
            log.warning(
                "Update deploy alias to {} for {}".format(
                    deploy_goal.deployAlias, deploy_goal.envName
                )
            )

    def _send_deploy_info_metrics(self) -> None:
        """
        Emit deploy info metrics to the tsd server
        """
        epoch_in_seconds = int(time.time())
        put_stmts = []
        for env_name, deploy_status in self._envs.items():
            report = deploy_status.report
            build_info = deploy_status.build_info

            if not report:
                log.info(
                    f"Skip deploy info metric for {env_name} due to missing report"
                )
                continue
            if not build_info:
                log.info(
                    f"Skip deploy info metric for {env_name} due to missing build_info"
                )
                continue
            if report.deployStage != DeployStage.SERVING_BUILD:
                log.info(
                    f"Skip deploy info metric for {env_name} for deploy stage {report.deployStage}"
                )
                continue
            if not build_info.build_commit:
                log.info(
                    f"Skip deploy info metric for {env_name} due to missing commit"
                )
                continue
            if not build_info.build_name:
                log.info(
                    f"Skip deploy info metric for {env_name} due to missing build_name"
                )
                continue

            put_stmts.append(
                f"put {DEPLOY_INFO_METRIC_NAME} "
                f"{epoch_in_seconds} {DEPLOY_INFO_METRIC_VALUE} "
                f"source=teletraan "
                f"artifact={build_info.build_name} "
                f"commit_sha={build_info.build_commit}"
            )

        if len(put_stmts) == 0:
            return

        # Add an empty statement to ensure the payload ends in a newline
        put_stmts.append("")

        tsd_host = self._config.get_tsd_host()
        tsd_port = self._config.get_tsd_port()
        tsd_timeout_seconds = self._config.get_tsd_timeout_seconds()

        sock = socket.socket()
        sock.settimeout(tsd_timeout_seconds)
        sock.connect((tsd_host, tsd_port))
        payload = "\n".join(put_stmts).encode("utf-8")
        sock.sendall(payload)

    @staticmethod
    def plan_changed(
        old_response, new_response
    ) -> Union[PingResponse, bool, DeployGoal]:
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
    parser.add_argument(
        "-e",
        "--server_stage",
        dest="stage",
        default="prod",
        help="This option is deprecated",
    )
    parser.add_argument(
        "-f",
        "--config-file",
        dest="config_file",
        required=False,
        help="the deploy agent config file path.",
    )
    parser.add_argument(
        "-d",
        "--daemon",
        dest="daemon",
        action="store_true",
        help="Run deploy agent in daemon mode. Default is false.",
    )
    parser.add_argument(
        "-n",
        "--host",
        dest="hostname",
        required=False,
        default=None,
        help="Host name being used when interact with Teletraan service. "
        "This is optional. By default the hostname defined in host-info "
        "file will be used",
    )
    parser.add_argument(
        "-g",
        "--group",
        dest="hostgroup",
        required=False,
        default=None,
        help="Group name being used when interact with Teletraan service. "
        "This is optional. By default the group name defined in host-info "
        "file will be used",
    )
    parser.add_argument(
        "--use-facter", dest="use_facter", action="store_true", default=False
    )
    parser.add_argument(
        "--use-host-info", dest="use_host_info", action="store_true", default=False
    )
    parser.add_argument(
        "-v",
        "--version",
        action="version",
        version=__version__,
        help="Deploy agent version.",
    )

    args: argparse.Namespace = parser.parse_args()

    config = Config(filenames=args.config_file)

    if IS_PINTEREST:
        import pinlogger

        pinlogger.initialize_logger(logger_filename="deploy-agent.log")
        pinlogger.LOG_TO_STDERR = True
    else:
        log_filename = os.path.join(config.get_log_directory(), "deploy-agent.log")
        logging.basicConfig(
            filename=log_filename,
            level=config.get_log_level(),
            format="%(asctime)s %(name)s:%(lineno)d %(levelname)s %(message)s",
        )

    if not check_prereqs(config):
        log.warning(
            "Deploy agent cannot start because the prerequisites on puppet did not meet."
        )
        sys.exit(0)

    log.info("Start to run deploy-agent.")
    # timing stats - agent start time
    create_sc_timing("deployd.stats.internal.time_start_sec", int(time.time()))
    client = Client(
        config=config,
        hostname=args.hostname,
        hostgroup=args.hostgroup,
        use_facter=args.use_facter,
        use_host_info=args.use_host_info,
    )

    uptime = utils_uptime()
    agent = DeployAgent(client=client, conf=config)
    create_sc_timing(
        "deployd.stats.ec2_uptime_sec", uptime, tags={"first_run": agent.first_run}
    )
    utils_listen()
    if args.daemon:
        agent.serve_forever()
    else:
        agent.serve_once()

    # timing stats - total processing time excluding external actions
    create_sc_timing(
        "deployd.stats.internal.time_elapsed_proc_sec",
        agent.stat_time_elapsed_internal.get(),
        tags={"first_run": agent.first_run},
    )
    # timing stats - agent total run time
    create_sc_timing(
        "deployd.stats.internal.time_elapsed_proc_total_sec",
        agent.stat_time_elapsed_total.get(),
        tags={"first_run": agent.first_run},
    )
    # timing stats - agent exit time
    create_sc_timing("deployd.stats.internal.time_end_sec", int(time.time()))


if __name__ == "__main__":
    main()
