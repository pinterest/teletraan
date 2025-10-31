import os
import tempfile
import shutil
import unittest

from unittest.mock import MagicMock, patch

# Patch LOCKFILE_DIR before importing DeployAgent to avoid /var/lock permission error
patch("deployd.common.single_instance.LOCKFILE_DIR", new="/tmp/lock").start()
from deployd.agent import DeployAgent  # noqa: E402


class TestScriptVariablesConfig(unittest.TestCase):
    def setUp(self):
        # Create a temp directory to act as the agent directory
        self.temp_dir = tempfile.mkdtemp()
        self.env_name = "testenv"
        self.script_config_path = os.path.join(
            self.temp_dir, f"{self.env_name}_SCRIPT_CONFIG"
        )

        # Mock config to return our temp dir
        self.mock_config = MagicMock()
        self.mock_config.get_agent_directory.return_value = self.temp_dir

        # Provide a mock executor with update_configs method
        self.mock_executor = MagicMock()

        # Create a minimal DeployAgent with the mocked config and executor
        self.agent = DeployAgent(
            client=MagicMock(), conf=self.mock_config, executor=self.mock_executor
        )

        # Patch log to avoid clutter
        patcher = patch("deployd.agent.log")
        self.addCleanup(patcher.stop)
        self.mock_log = patcher.start()

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def test_script_variables_file_created(self):
        # Simulate a deploy_goal with scriptVariables
        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = {"FOO": "bar", "BAZ": "qux"}
        deploy_goal.deployId = "dummy"

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        with open(self.script_config_path) as f:
            content = f.read()
        self.assertIn("FOO=bar", content)
        self.assertIn("BAZ=qux", content)

    def test_script_variables_file_removed(self):
        # First, create the file
        with open(self.script_config_path, "w") as f:
            f.write("SHOULD_BE_REMOVED=1\n")

        # Simulate a deploy_goal with no scriptVariables
        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "dummy"

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        self.assertFalse(os.path.exists(self.script_config_path))


if __name__ == "__main__":
    unittest.main()
