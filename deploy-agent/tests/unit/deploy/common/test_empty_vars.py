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

    def test_script_variables_file_removed_on_new_deploy(self):
        """File should be removed when a NEW deploy goal has no scriptVariables."""
        with open(self.script_config_path, "w") as f:
            f.write("SHOULD_BE_REMOVED=1\n")

        # Set a previous deploy goal so the new one is detected as different
        previous_goal = MagicMock()
        previous_goal.envName = self.env_name
        self.agent.deploy_goal_previous = previous_goal

        # Simulate a NEW deploy_goal with no scriptVariables
        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "deploy-2"

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        self.assertFalse(os.path.exists(self.script_config_path))

    def test_script_variables_file_preserved_on_same_deploy(self):
        """File should NOT be removed on a routine ping with the same deploy goal.

        This is the key scenario that caused the incident: on every ping cycle the
        server returns scriptVariables=None for an unchanged deploy.  The config
        file must be left intact so that containers keep using the previously
        written variables.
        """
        with open(self.script_config_path, "w") as f:
            f.write("EXISTING_VAR=1\n")

        # Simulate a deploy_goal with no scriptVariables
        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "deploy-1"

        # Set previous goal to the SAME object (routine ping, no change)
        self.agent.deploy_goal_previous = deploy_goal

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        # File must still exist — this is what the buggy code broke
        self.assertTrue(os.path.exists(self.script_config_path))
        with open(self.script_config_path) as f:
            content = f.read()
        self.assertIn("EXISTING_VAR=1", content)

    def test_script_variables_file_removed_when_no_previous_goal(self):
        """File should be removed when there is no previous deploy goal (first deploy)."""
        with open(self.script_config_path, "w") as f:
            f.write("STALE_VAR=1\n")

        # No previous deploy goal (agent just started)
        self.agent.deploy_goal_previous = None

        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "deploy-1"

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        self.assertFalse(os.path.exists(self.script_config_path))

    def test_script_variables_file_not_present_no_error(self):
        """No error when file doesn't exist and scriptVariables is empty on new deploy."""
        # Ensure no file exists
        if os.path.exists(self.script_config_path):
            os.remove(self.script_config_path)

        previous_goal = MagicMock()
        self.agent.deploy_goal_previous = previous_goal

        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "deploy-2"

        response = MagicMock()
        response.deployGoal = deploy_goal

        # Should not raise
        self.agent._update_internal_deploy_goal(response)
        self.assertFalse(os.path.exists(self.script_config_path))


if __name__ == "__main__":
    unittest.main()
