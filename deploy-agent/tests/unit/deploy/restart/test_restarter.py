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

import signal
import subprocess
import unittest
from unittest import mock

from deployd.restart.restarter import Restarter


class TestRestarter(unittest.TestCase):
    @mock.patch("os.kill")
    @mock.patch("subprocess.run")
    def test_restarter(self, subprocess_run_mock, os_kill_mock):
        # Mock the calls to os.kill
        os_kill_mock.side_effect = [None, None, ProcessLookupError]

        # Mock the calls to subprocess.run
        crontab_stdout = (
            "* * * * * AWS_EC2_METADATA_V1_DISABLED=true PINLOG_MIN_LOG_LEVEL=DEBUG "
            "PINLOG_LOG_TO_STDERR=1 PINLOG_STDERR_LEVEL=DEBUG PINLOG_LOG_DIR=/var/log/deployd "
            "IS_PINTEREST=true METRIC_PORT_HEALTH=18126 METRIC_CACHE_PATH=/mnt/deployd/metrics.cache "
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/local/bin/deploy-agent "
            "-e prod -f /etc/deployagent.conf --use-facter >/dev/null 2>&1"
        )
        subprocess_run_mock.side_effect = [
            subprocess.CompletedProcess([], 0, stdout=crontab_stdout),
            subprocess.CompletedProcess([], 0),
        ]

        # Run the restarter
        test_pid = "123123123"
        test_ping_file = "/tmp/initial-ping.json"
        restarter = Restarter(test_pid, test_ping_file)
        restarter.restart()

        # Verify the calls to os.kill
        os_kill_calls = [
            mock.call(int(test_pid), signal.SIGTERM),
            mock.call(int(test_pid), 0),
            mock.call(int(test_pid), signal.SIGTERM),
        ]
        os_kill_mock.assert_has_calls(os_kill_calls)

        # Verify the calls to subprocess.run
        deploy_agent_cmd = (
            "AWS_EC2_METADATA_V1_DISABLED=true PINLOG_MIN_LOG_LEVEL=DEBUG "
            "PINLOG_LOG_TO_STDERR=1 PINLOG_STDERR_LEVEL=DEBUG PINLOG_LOG_DIR=/var/log/deployd "
            "IS_PINTEREST=true METRIC_PORT_HEALTH=18126 METRIC_CACHE_PATH=/mnt/deployd/metrics.cache "
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin /usr/local/bin/deploy-agent "
            f"-e prod -i {test_ping_file} -f /etc/deployagent.conf --use-facter >/dev/null 2>&1"
        )
        subprocess_run_calls = [
            mock.call(["crontab", "-l"], capture_output=True, check=True, text=True),
            mock.call(
                deploy_agent_cmd,
                shell=True,
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            ),
        ]
        subprocess_run_mock.assert_has_calls(subprocess_run_calls)
