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

import unittest
from unittest import mock
from tests import TestCase

from deployd.common.utils import (
    ensure_dirs,
    check_prereqs,
    check_first_puppet_run_success,
)


@mock.patch(
    "deployd.common.utils.send_statsboard_metric", new=mock.Mock(return_value=None)
)
class TestCommonUtils(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.estatus = mock.Mock()
        cls.estatus.load_envs = mock.Mock(return_value=None)
        cls.config = mock.Mock()
        cls.config.load_env_and_configs = mock.Mock()
        cls.config.get_agent_directory = mock.Mock(return_value="/tmp/deployd/")
        cls.config.get_builds_directory = mock.Mock(return_value="/tmp/deployd/builds/")
        cls.config.get_log_directory = mock.Mock(return_value="/tmp/logs/")
        cls.config.respect_puppet = mock.Mock(return_value=True)
        cls.config.get_puppet_state_file_path = mock.Mock(return_value="/tmp/deployd")
        ensure_dirs(cls.config)

    @mock.patch("deployd.common.utils.IS_PINTEREST", False)
    def test_check_prereqs_not_pins(self):
        result = check_prereqs(self.config)
        self.assertTrue(result)

    @mock.patch("deployd.common.utils.IS_PINTEREST", True)
    @mock.patch("deployd.common.utils.is_first_run", new=mock.Mock(return_value=True))
    @mock.patch(
        "deployd.common.utils.load_puppet_summary",
        new=mock.Mock(return_value={"events": {"failure": 0}}),
    )
    @mock.patch(
        "deployd.common.utils.get_puppet_exit_code", new=mock.Mock(return_value=5)
    )
    def test_check_prereqs_no_failures(self):
        first_puppet_run_result = check_first_puppet_run_success(self.config)
        self.assertTrue(first_puppet_run_result)
        result = check_prereqs(self.config)
        self.assertTrue(result)

    @mock.patch("deployd.common.utils.IS_PINTEREST", True)
    @mock.patch("deployd.common.utils.is_first_run", new=mock.Mock(return_value=True))
    @mock.patch(
        "deployd.common.utils.load_puppet_summary",
        new=mock.Mock(return_value={"events": {"failure": 0}}),
    )
    @mock.patch(
        "deployd.common.utils.get_puppet_exit_code", new=mock.Mock(return_value=999)
    )
    def test_check_prereqs_no_exit_code_file(self):
        result = check_prereqs(self.config)
        self.assertTrue(result)

    @mock.patch("deployd.common.utils.IS_PINTEREST", True)
    @mock.patch("deployd.common.utils.is_first_run", new=mock.Mock(return_value=True))
    @mock.patch(
        "deployd.common.utils.load_puppet_summary",
        new=mock.Mock(return_value={"events": {"failure": 3}}),
    )
    @mock.patch(
        "deployd.common.utils.get_puppet_exit_code", new=mock.Mock(return_value=999)
    )
    def test_check_prereqs_with_failures(self):
        first_puppet_run_result = check_first_puppet_run_success(self.config)
        self.assertFalse(first_puppet_run_result)
        result = check_prereqs(self.config)
        self.assertFalse(result)

    @mock.patch("deployd.common.utils.IS_PINTEREST", True)
    @mock.patch("deployd.common.utils.is_first_run", new=mock.Mock(return_value=False))
    @mock.patch(
        "deployd.common.utils.load_puppet_summary",
        new=mock.Mock(return_value={"events": {"failure": 2}}),
    )
    @mock.patch(
        "deployd.common.utils.get_puppet_exit_code", new=mock.Mock(return_value=999)
    )
    def test_check_prereqs_no_state_file(self):
        self.config.get_puppet_state_file_path = mock.Mock(return_value=None)
        result = check_prereqs(self.config)
        self.assertTrue(result)

    @mock.patch("deployd.common.utils.IS_PINTEREST", True)
    @mock.patch("deployd.common.utils.is_first_run", new=mock.Mock(return_value=False))
    def test_check_prereqs_not_first_run(self):
        result = check_prereqs(self.config)
        self.assertTrue(result)


if __name__ == "__main__":
    unittest.main()
