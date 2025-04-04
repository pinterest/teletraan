# Copyright 2025 Pinterest, Inc.
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
import logging
import os
import signal
import subprocess
import time

from deployd.common.config import Config
from deployd.common import LOG_FORMAT

log = logging.getLogger(__name__)

KILL_WAIT_MIN_INTERVAL = 1 / 1024  # seconds
KILL_WAIT_MAX_INTERVAL = 1  # seconds
KILL_WAIT_BACKOFF_FACTOR = 2
KILL_WAIT_RETRIES = 30


class Restarter:
    def __init__(self, deploy_agent_pid, initial_ping_file):
        self.deploy_agent_pid = deploy_agent_pid
        self.initial_ping_file = initial_ping_file

    def restart(self):
        log.info(
            "Restarting deploy-agent parent process "
            f"deploy_agent_pid={self.deploy_agent_pid} "
            f"initial_ping_file={self.initial_ping_file}"
        )

        self._kill_deploy_agent_process()
        self._start_deploy_agent_process()

    def _kill_deploy_agent_process(self):
        deploy_agent_pid = int(self.deploy_agent_pid)
        num_retries = KILL_WAIT_RETRIES
        interval = KILL_WAIT_MIN_INTERVAL

        while True:
            try:
                os.kill(deploy_agent_pid, signal.SIGTERM)
                os.kill(deploy_agent_pid, 0)
            except ProcessLookupError:
                log.info(
                    f"Successfully killed deploy agent process: {deploy_agent_pid}"
                )
                return

            if num_retries <= 0:
                raise Exception(
                    f"Failed waiting for deploy agent pid to be killed: {deploy_agent_pid}"
                )
            num_retries -= 1

            log.info(f"Waiting for process {deploy_agent_pid} to be killed")
            time.sleep(interval)
            interval *= KILL_WAIT_BACKOFF_FACTOR
            interval = min(interval, KILL_WAIT_MAX_INTERVAL)

    def _start_deploy_agent_process(self):
        crontab_cmd = ["crontab", "-l"]
        crontab_process = subprocess.run(
            crontab_cmd, capture_output=True, check=True, text=True
        )
        if crontab_process.stderr:
            log.warning(
                f"Got error output from crontab command: {crontab_process.stderr}"
            )
        for crontab_line in crontab_process.stdout.splitlines():
            if "/usr/local/bin/deploy-agent" in crontab_line:
                crontab_parts = crontab_line.split()
                crontab_parts = crontab_parts[5:]  # Ignore the cron schedule prefix

                deploy_agent_cmd = []
                for crontab_part in crontab_parts:
                    if (
                        crontab_part == "-f" or crontab_part == "--config-file"
                    ) and self.initial_ping_file:
                        log.info(
                            f"Adding initial ping file to deploy-agent command: {self.initial_ping_file}"
                        )
                        deploy_agent_cmd.append("-i")
                        deploy_agent_cmd.append(self.initial_ping_file)

                    deploy_agent_cmd.append(crontab_part)

                log.info(f"Executing deploy-agent command: {deploy_agent_cmd}")
                subprocess.run(
                    " ".join(deploy_agent_cmd),
                    shell=True,
                    check=True,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
                log.info("Successfully ran deploy-agent command")
                break


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "-f",
        "--config-file",
        dest="config_file",
        required=True,
        help="the deploy agent config file path.",
    )
    parser.add_argument(
        "-i",
        "--initial-ping-file",
        dest="initial_ping_file",
        required=False,
        help="The file path containing the initial Teletraan ping to use for the restart",
    )
    parser.add_argument(
        "-p",
        "--pid",
        required=True,
        help="The deploy-agent process id which will be killed",
    )
    args = parser.parse_args()
    config = Config(args.config_file)
    logging.basicConfig(format=LOG_FORMAT, level=config.get_log_level())

    log.info("Start to restart the deploy-agent")
    try:
        restarter = Restarter(args.pid, args.initial_ping_file)
        restarter.restart()
    except Exception:
        log.exception("Failed to restart the deploy-agent")


if __name__ == "__main__":
    main()
