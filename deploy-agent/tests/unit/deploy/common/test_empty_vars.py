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
        """File is created when scriptVariables is a non-empty dict."""
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

    def test_script_variables_file_removed_when_empty_dict(self):
        """File should be removed when scriptVariables is an explicit empty dict {}.

        The server sends {} on the first deploy stage (PRE_DOWNLOAD) when the
        user has cleared all script variables.  This is distinct from None which
        means 'not included in this response'.
        """
        with open(self.script_config_path, "w") as f:
            f.write("SHOULD_BE_REMOVED=1\n")

        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = {}  # explicit empty dict from server
        deploy_goal.deployId = "deploy-2"

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        self.assertFalse(os.path.exists(self.script_config_path))

    def test_script_variables_file_preserved_when_none(self):
        """File should NOT be removed when scriptVariables is None.

        None means the server did not include scriptVariables in this response
        (e.g. it is not the first deploy stage).  The previously written config
        file must be left intact so that containers keep using the variables.
        This is the key scenario that caused the incident.
        """
        with open(self.script_config_path, "w") as f:
            f.write("EXISTING_VAR=1\n")

        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "deploy-1"

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        # File must still exist
        self.assertTrue(os.path.exists(self.script_config_path))
        with open(self.script_config_path) as f:
            content = f.read()
        self.assertIn("EXISTING_VAR=1", content)

    def test_script_variables_file_preserved_on_same_deploy_none(self):
        """File preserved when same deploy goal pings again with scriptVariables=None.

        On routine ping cycles the server returns scriptVariables=None for an
        unchanged deploy.  The config file must be left intact.
        """
        with open(self.script_config_path, "w") as f:
            f.write("EXISTING_VAR=1\n")

        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "deploy-1"

        # Set previous goal to the SAME object (routine ping, no change)
        self.agent.deploy_goal_previous = deploy_goal

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        self.assertTrue(os.path.exists(self.script_config_path))
        with open(self.script_config_path) as f:
            content = f.read()
        self.assertIn("EXISTING_VAR=1", content)

    def test_script_variables_file_preserved_on_next_stage(self):
        """File preserved when deploy advances to next stage with scriptVariables=None.

        The server only sends scriptVariables during PRE_DOWNLOAD.  On DOWNLOADING
        and later stages, scriptVariables is None.  Even though the deploy goal
        changed (different stage), the config file must NOT be deleted.
        """
        with open(self.script_config_path, "w") as f:
            f.write("V=1\n")

        # Simulate a previous goal from PRE_DOWNLOAD stage
        previous_goal = MagicMock()
        previous_goal.envName = self.env_name
        previous_goal.scriptVariables = {"V": "1"}
        self.agent.deploy_goal_previous = previous_goal

        # Now the agent gets the DOWNLOADING stage — scriptVariables is None
        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "deploy-1"

        response = MagicMock()
        response.deployGoal = deploy_goal

        self.agent._update_internal_deploy_goal(response)

        # File must still exist — this is critical
        self.assertTrue(os.path.exists(self.script_config_path))
        with open(self.script_config_path) as f:
            content = f.read()
        self.assertIn("V=1", content)

    def test_empty_dict_no_file_no_error(self):
        """No error when file doesn't exist and scriptVariables is empty dict."""
        if os.path.exists(self.script_config_path):
            os.remove(self.script_config_path)

        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = {}
        deploy_goal.deployId = "deploy-2"

        response = MagicMock()
        response.deployGoal = deploy_goal

        # Should not raise
        self.agent._update_internal_deploy_goal(response)
        self.assertFalse(os.path.exists(self.script_config_path))

    def test_none_no_file_no_error(self):
        """No error when file doesn't exist and scriptVariables is None."""
        if os.path.exists(self.script_config_path):
            os.remove(self.script_config_path)

        deploy_goal = MagicMock()
        deploy_goal.envName = self.env_name
        deploy_goal.scriptVariables = None
        deploy_goal.deployId = "deploy-1"

        response = MagicMock()
        response.deployGoal = deploy_goal

        # Should not raise, file should still not exist
        self.agent._update_internal_deploy_goal(response)
        self.assertFalse(os.path.exists(self.script_config_path))


if __name__ == "__main__":
    unittest.main()
