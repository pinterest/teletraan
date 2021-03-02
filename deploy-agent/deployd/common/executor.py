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

import datetime
import subprocess
import logging
import os
import signal
import stat
import time
import traceback

from deployd.common.types import DeployReport, PingStatus, PRE_STAGE_STEPS, AgentStatus

log = logging.getLogger(__name__)


class Executor(object):
    def __init__(self, callback=None, config=None):
        self._ping_server = callback
        if not config:
            return

        self._config = config
        self.update_configs(config)

    def update_configs(self, config):
        self.LOG_FILENAME = config.get_subprocess_log_name()
        self.MAX_RUNNING_TIME = config.get_subprocess_running_timeout()
        self.MIN_RUNNING_TIME = config.get_agent_ping_interval()
        self.MAX_RETRY = config.get_subproces_max_retry()
        self.MAX_TAIL_BYTES = config.get_subprocess_max_log_bytes()
        self.PROCESS_POLL_INTERVAL = config.get_subprocess_poll_interval()
        self.BACK_OFF = config.get_backoff_factor()
        self.MAX_SLEEP_INTERVAL = config.get_subprocess_max_sleep_interval()
        self._config = config
        log.debug('Executor configs have been updated: '
                  'PING_INTERVAL={}, TIME_OUT={}, MAX_RETRY={}'.format(self.MIN_RUNNING_TIME,
                                                                      self.MAX_RUNNING_TIME,
                                                                      self.MAX_RETRY))

    def get_subprocess_output(self, fd, file_pos):
        curr_pos = fd.tell()
        fd.seek(file_pos, 0)
        output = fd.read()
        fd.seek(curr_pos, 0)
        return output[-(self.MAX_TAIL_BYTES+1):-1]

    def run_cmd(self, cmd, **kw):
        if not isinstance(cmd, list):
            cmd = cmd.split(' ')
        cmd_str = ' '.join(cmd)
        log.info('Running: {} with {} retries.'.format(cmd_str, self.MAX_RETRY))

        deploy_report = DeployReport(status_code=AgentStatus.UNKNOWN,
                                     error_code=0,
                                     retry_times=0)
        process_interval = self.PROCESS_POLL_INTERVAL
        start = datetime.datetime.now()
        init_start = datetime.datetime.now()
        total_retry = 0

        with open(self.LOG_FILENAME, 'a+') as fdout:
            while total_retry < self.MAX_RETRY:
                try:
                    fdout.seek(0, 2)
                    file_pos = fdout.tell()
                    process = subprocess.Popen(cmd, stdout=fdout, stderr=fdout,
                                               preexec_fn=os.setsid, **kw)
                    while process.poll() is None:
                        start, deploy_report = \
                            self.ping_server_if_possible(start, cmd, deploy_report)
                        """
                        terminate case 1:
                        the server changed the deploy goal, return to the agent to handle next
                        deploy step
                        """
                        if deploy_report.status_code == AgentStatus.ABORTED_BY_SERVER:
                            Executor._kill_process(process)
                            return deploy_report

                        """
                        terminate case 2:
                        the script gets timeout error, return to the agent to report to the server
                        """
                        if (datetime.datetime.now() - init_start).seconds >= self.MAX_RUNNING_TIME:
                            Executor._kill_process(process)
                            # the best way to get output is to tail the log
                            deploy_report.output_msg = self.get_subprocess_output(fd=fdout,
                                                                                  file_pos=file_pos)
                            log.info("Exceed max running time: {}.".format(self.MAX_RUNNING_TIME))
                            log.info("Output from subprocess: {}".format(deploy_report.output_msg))
                            deploy_report.status_code = AgentStatus.SCRIPT_TIMEOUT
                            deploy_report.error_code = 1
                            return deploy_report

                        # sleep some seconds before next poll
                        sleep_time = self._get_sleep_interval(start, self.PROCESS_POLL_INTERVAL)
                        time.sleep(sleep_time)

                    # finish executing sub process
                    deploy_report.error_code = process.returncode
                    deploy_report.output_msg = self.get_subprocess_output(fd=fdout,
                                                                          file_pos=file_pos)
                    if process.returncode == 0:
                        log.info('Running: {} succeeded.'.format(cmd_str))
                        deploy_report.status_code = AgentStatus.SUCCEEDED
                        return deploy_report
                except Exception:
                    error_msg = traceback.format_exc()
                    deploy_report.error_code = 1
                    deploy_report.output_msg = error_msg
                    log.error(error_msg)

                # fails when:
                # subprocess execution fails
                # popen throws
                deploy_report.status_code = AgentStatus.SCRIPT_FAILED
                deploy_report.retry_times += 1
                total_retry += 1

                """
                Terminate case 3:
                Too many failed retries, return to the agent and report to the server.
                """
                if total_retry >= self.MAX_RETRY:
                    deploy_report.status_code = AgentStatus.TOO_MANY_RETRY
                    return deploy_report

                init_start = datetime.datetime.now()  # reset the initial start time

                log.info('Failed: {}, at {} retry. Error:\n{}'.format(cmd_str,
                                                                      deploy_report.retry_times,
                                                                      deploy_report.output_msg))
                sleep_time = self._get_sleep_interval(start, process_interval)
                time.sleep(sleep_time)
                start, deploy_report = self.ping_server_if_possible(start, cmd, deploy_report)
                if deploy_report.status_code == AgentStatus.ABORTED_BY_SERVER:
                    return deploy_report

                # sleep the rest of the time
                if process_interval - sleep_time > 0:
                    time.sleep(process_interval - sleep_time)
                # exponential backoff
                process_interval = min(process_interval * self.BACK_OFF, self.MAX_SLEEP_INTERVAL)

        deploy_report.status_code = AgentStatus.TOO_MANY_RETRY
        return deploy_report

    def ping_server_if_possible(self, start, cmd_str, deploy_report):
        now = datetime.datetime.now()
        processed_time = (now - start).seconds
        log.debug("start: {}, now: {}, process: {}".format(start, now, processed_time))
        if processed_time >= self.MIN_RUNNING_TIME and self._ping_server:
            start = now
            log.info('Exceed min running time: {}, '
                     'reporting to the server'.format(self.MIN_RUNNING_TIME))
            result = self._ping_server(deploy_report)
            if result == PingStatus.PLAN_CHANGED:
                deploy_report.status_code = AgentStatus.ABORTED_BY_SERVER
                log.info('Deploy goal has changed, '
                         'aborting the current command {}.'.format(' '.join(cmd_str)))

        return start, deploy_report

    def _get_sleep_interval(self, start, interval):
        now = datetime.datetime.now()
        max_sleep_seconds = self.MIN_RUNNING_TIME - (now - start).seconds
        return min(interval, max(max_sleep_seconds, 1))

    @staticmethod
    def _kill_process(process):
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except Exception as e:
            log.debug('Failed to kill process: {}'.format(e))

    def execute_command(self, script):
        try:
            deploy_step = os.getenv('DEPLOY_STEP')
            if not os.path.exists(self._config.get_script_directory()):
                """if the teletraan directory does not exist in the pre stage steps. It
                means it's a newly added host (never deployed before). Show a warning message
                and exit. Otherwise, we treat it as an agent failure (nothing to execute)
                """
                error_msg = "teletraan directory cannot be found " \
                            "in the tar ball in step {}!".format(deploy_step)
                if deploy_step in PRE_STAGE_STEPS:
                    log.warning(error_msg)
                    return DeployReport(status_code=AgentStatus.SUCCEEDED)
                else:
                    log.error(error_msg)
                    return DeployReport(status_code=AgentStatus.AGENT_FAILED, error_code=1,
                                        retry_times=1, output_msg=error_msg)

            script = os.path.join(self._config.get_script_directory(), script)
            if not os.path.exists(script):
                if deploy_step == 'RESTARTING':
                    # RESTARTING script is required
                    error_msg = 'RESTARTING script does not exist.'
                    log.error(error_msg)
                    return DeployReport(status_code=AgentStatus.AGENT_FAILED, error_code=1,
                                        retry_times=1, output_msg=error_msg)
                else:
                    log.info('script: {} does not exist.'.format(script))
                    return DeployReport(status_code=AgentStatus.SUCCEEDED)

            os.chdir(self._config.get_script_directory())
            # change the mode of the script
            st = os.stat(script)
            os.chmod(script, st.st_mode | stat.S_IXUSR)
            return self.run_cmd(script)
        except Exception as e:
            error_msg = str(e)
            log.error('Failed to execute command: {}. Reason: {}'.format(script, error_msg))
            log.error(traceback.format_exc())
            return DeployReport(status_code=AgentStatus.AGENT_FAILED,
                                error_code=1,
                                output_msg=str(e))
