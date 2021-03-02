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

import os
import tempfile
import unittest
import mock
import tests

from deployd.common.executor import Executor
from deployd.common.types import AgentStatus


class TestUtilsFunctions(tests.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fdout, cls.fdout_fn = tempfile.mkstemp()
        cls.pingServer = mock.Mock()
        cls.pingServer.__call__ = mock.Mock(return_value=False)
        cls.executor = Executor(callback=cls.pingServer)
        cls.executor.LOG_FILENAME = cls.fdout_fn

    @classmethod
    def tearDownClass(cls):
        os.close(cls.fdout)
        os.remove(cls.fdout_fn)

    def test_run_bad_script(self):
        fdout_fn = tempfile.mkstemp()[1]
        with open(fdout_fn, 'w') as f:
            f.write('echo hello')
        os.chmod(fdout_fn, 0o755)

        ping_server = mock.Mock(return_value=True)
        executor = Executor(callback=ping_server)
        executor.LOG_FILENAME = self.fdout_fn
        executor.MAX_RUNNING_TIME = 4
        executor.MIN_RUNNING_TIME = 2
        executor.DEFAULT_TAIL_LINES = 1
        executor.PROCESS_POLL_INTERVAL = 2
        executor.MAX_RETRY = 3
        deploy_report = executor.run_cmd(cmd=fdout_fn)
        self.assertTrue(ping_server.called)
        self.assertEqual(deploy_report.status_code, AgentStatus.ABORTED_BY_SERVER)
        os.remove(fdout_fn)

    def test_run_command(self):
        cmd = ['echo', 'hello world']
        self.executor.MAX_RUNNING_TIME = 5
        self.executor.MAX_RETRY = 3
        self.executor.PROCESS_POLL_INTERVAL = 2
        self.executor.MIN_RUNNING_TIME = 2
        self.executor.BACK_OFF = 2
        self.executor.MAX_SLEEP_INTERVAL = 60
        self.executor.MAX_TAIL_BYTES = 10240
        self.executor.LOG_FILENAME = self.fdout_fn
        deploy_report = self.executor.run_cmd(cmd=cmd)
        self.assertEqual(deploy_report.status_code, AgentStatus.SUCCEEDED)

    def test_run_command_with_big_output(self):
        cmd = ['seq', '1000000']
        self.executor.MIN_RUNNING_TIME = 2
        deploy_report = self.executor.run_cmd(cmd=cmd)
        self.assertEqual(deploy_report.status_code, AgentStatus.SUCCEEDED)
        self.assertIsNotNone(deploy_report.output_msg)

    def test_run_command_with_max_retry(self):
        cmd = ['ls', '-ltr', '/abc']
        ping_server = mock.Mock(return_value=False)
        executor = Executor(callback=ping_server)
        executor.LOG_FILENAME = self.fdout_fn
        executor.MAX_RUNNING_TIME = 5
        executor.MIN_RUNNING_TIME = 2
        executor.MAX_RETRY = 3
        executor.DEFAULT_TAIL_LINES = 1
        executor.PROCESS_POLL_INTERVAL = 2
        executor.BACK_OFF = 2
        executor.MAX_SLEEP_INTERVAL = 60
        executor.MAX_TAIL_BYTES = 10240
        deploy_report = executor.run_cmd(cmd=cmd)
        self.assertEqual(deploy_report.status_code, AgentStatus.TOO_MANY_RETRY)
        # in ubuntu: error message is 'ls: cannot access /abc: No such file or directory'
        # in mac osx: error message is 'ls: /abc: No such file or directory'
        self.assertEqual(deploy_report.retry_times, 3)

    def test_run_command_with_timeout(self):
        cmd = ['ls', '-ltr', '/abc']
        ping_server = mock.Mock(return_value=True)
        executor = Executor(callback=ping_server)
        executor.LOG_FILENAME = self.fdout_fn
        executor.MAX_RUNNING_TIME = 4
        executor.MIN_RUNNING_TIME = 2
        executor.DEFAULT_TAIL_LINES = 1
        executor.MAX_RETRY = 3
        executor.PROCESS_POLL_INTERVAL = 2
        executor.MAX_TAIL_BYTES = 10240
        deploy_report = executor.run_cmd(cmd=cmd)
        self.assertEqual(deploy_report.status_code, AgentStatus.ABORTED_BY_SERVER)

    def test_run_command_with_timeout_error(self):
        cmd = ['sleep', '20']
        ping_server = mock.Mock(return_value=False)
        executor = Executor(callback=ping_server)
        executor.LOG_FILENAME = self.fdout_fn
        executor.MAX_RUNNING_TIME = 4
        executor.MIN_RUNNING_TIME = 2
        executor.DEFAULT_TAIL_LINES = 1
        executor.MAX_RETRY = 3
        executor.PROCESS_POLL_INTERVAL = 2
        executor.BACK_OFF = 2
        executor.MAX_SLEEP_INTERVAL = 60
        executor.MAX_TAIL_BYTES = 10240
        deploy_report = executor.run_cmd(cmd=cmd)
        self.assertTrue(ping_server.called)
        self.assertEqual(deploy_report.status_code, AgentStatus.SCRIPT_TIMEOUT)


if __name__ == '__main__':
    unittest.main()
