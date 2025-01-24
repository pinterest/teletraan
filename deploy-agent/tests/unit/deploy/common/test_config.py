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

from unittest import mock
import os
import os.path
import tempfile
import unittest
import tests

from deployd.common.config import Config
from deployd.common.types import DeployStatus, DeployType, OpCode, DeployStage
from deployd.types.ping_response import PingResponse


class TestConfigFunctions(tests.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.dirname = tempfile.mkdtemp()
        env_filename = os.path.join(cls.dirname, "variables")
        lines = ['env1 = "test1"\n', "env2 =   'test2'\n", "env3 = test3\n"]

        with open(env_filename, "w") as f:
            f.writelines(lines)

        config_reader = mock.Mock()
        config_reader.get = mock.Mock(return_value="/tmp")
        cls.config = Config(config_reader=config_reader)

    def test_get_target(self):
        deploy_goal = {}
        deploy_goal["deployId"] = "123"
        deploy_goal["stageName"] = "beta"
        deploy_goal["envName"] = "pinboard"
        deploy_goal["stageType"] = "DEFAULT"
        deploy_goal["deployStage"] = DeployStage.SERVING_BUILD
        ping_response = {"deployGoal": deploy_goal, "opCode": OpCode.NOOP}

        response = PingResponse(jsonValue=ping_response)
        self.config.update_variables(DeployStatus(response))
        self.assertEqual(os.environ["DEPLOY_ID"], "123")
        self.assertEqual(os.environ["ENV_NAME"], "pinboard")
        self.assertEqual(os.environ["STAGE_NAME"], "beta")
        self.assertEqual(os.environ["COMPUTE_ENV_TYPE"], "PRODUCTION")
        self.assertEqual(self.config.get_target(), "/tmp/pinboard")

    def test_init(self):
        Config()

        with self.assertRaises(TypeError):
            Config(
                filenames=[
                    os.path.join(self.dirname, "test_file1.conf"),
                    os.path.join(self.dirname, "test_file2.conf"),
                ]
            )

        open(os.path.join(self.dirname, "test_file1.conf"), "w")
        Config(filenames=os.path.join(self.dirname, "test_file1.conf"))

        os.remove(os.path.join(self.dirname, "test_file1.conf"))
        with self.assertRaises(SystemExit), mock.patch("builtins.print") as print_mock:
            Config(filenames=os.path.join(self.dirname, "test_file1.conf"))
            print_mock.assert_called_once_with(
                f"Cannot find config files: {os.path.join(self.dirname, 'test_file1.conf')}"
            )

    def test_get_config_filename(self):
        filename = os.path.join(self.dirname, "test_file1.conf")
        open(os.path.join(self.dirname, "test_file1.conf"), "w")

        config = Config(filenames=filename)
        self.assertEqual(config.get_config_filename(), filename)

    def test_get_deploy_type_from_op_code(self):
        config = Config()
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="NOOP"), DeployType.REGULAR
        )
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="DEPLOY"), DeployType.REGULAR
        )
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="UPDATE"), DeployType.REGULAR
        )
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="RESTART"), DeployType.RESTART
        )
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="DELETE"), DeployType.REGULAR
        )
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="TERMINATE"), DeployType.STOP
        )
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="WAIT"), DeployType.REGULAR
        )
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="ROLLBACK"), DeployType.ROLLBACK
        )
        self.assertEqual(
            config._get_deploy_type_from_opcode(opCode="STOP"), DeployType.STOP
        )

    def test_config_first_run(self):
        config = Config()
        with mock.patch("os.path.exists") as os_path_exists:
            # first run
            os_path_exists.return_value = False
            self.assertTrue(config.first_run)

            # first run stickiness
            os_path_exists.return_value = True
            self.assertTrue(config.first_run)

            # subsequent run
            self.assertFalse(Config().first_run)


if __name__ == "__main__":
    unittest.main()
