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

# -*- coding: utf-8 -*-
"""
This is a positive ping test to go through a success deploy
"""

import unittest
import commons

systems_helper = commons.get_system_helper()
environs_helper = commons.get_environ_helper()
deploys_helper = commons.get_deploy_helper()
agents_helper = commons.get_agent_helper()
stageName = "prod"


class TestPings(unittest.TestCase):
    def setUp(self):
        self.envName = "ping-test-env-" + commons.gen_random_num()
        self.commit = commons.gen_random_num(32)
        self.host = "ping-test-host-" + commons.gen_random_num()
        self.group = "ping-test-group-" + commons.gen_random_num()
        self.groups = [self.group]
        self.env = commons.create_env(self.envName, stageName)
        environs_helper.update_env_capacity(
            commons.REQUEST, self.envName, stageName, data=self.groups
        )
        environs_helper.update_env_basic_config(
            commons.REQUEST, self.envName, stageName, {"maxParallel": 5}
        )
        environs_helper.update_env_script_config(
            commons.REQUEST, self.envName, stageName, {"s-c-n": "s-c-v"}
        )
        environs_helper.update_env_agent_config(
            commons.REQUEST, self.envName, stageName, {"a-c-n": "a-c-v"}
        )
        self.build = commons.publish_build("ping-test", "master", commit=self.commit)
        self.deploy = deploys_helper.deploy(
            commons.REQUEST, self.envName, stageName, self.build["id"]
        )

    def tearDown(self):
        commons.delete_env(self.envName, stageName)
        commons.delete_build(self.build["id"])
        commons.delete_deploy(self.deploy["id"])

    def ping(
        self,
        host=None,
        groups=None,
        envId=None,
        deployId=None,
        stage="UNKNOWN",
        status="UNKNOWN",
        error_code=0,
        error_message=None,
        deploy_alias=None,
        fail_count=0,
    ):
        ip = "8.8.8.8"
        reports = {}

        pingRequest = {}
        pingRequest["hostId"] = host
        pingRequest["hostName"] = host
        pingRequest["hostIp"] = ip
        pingRequest["groups"] = groups
        pingRequest["reports"] = reports

        report = {}
        report["envId"] = envId
        report["deployId"] = deployId
        report["deployStage"] = stage
        report["agentStatus"] = status
        report["errorCode"] = error_code
        report["errorMessage"] = error_message
        report["deployAlias"] = deploy_alias
        report["failCount"] = fail_count
        pingRequest["reports"] = [report]

        return systems_helper.ping(commons.REQUEST, pingRequest)

    def test_ping_empty_group(self):
        response = self.ping(self.host)
        self.assertEqual(response["opCode"], "NOOP")
        self.assertTrue(response.get("deployGoal") is None)

    def test_deploy_onhold(self):
        deploys_helper.pause(commons.REQUEST, self.envName, stageName)
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "RESTARTING",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "NOOP")
        self.assertTrue(response.get("deployGoal") is None)

    def test_agent_onhold(self):
        self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "PRE_RESTART",
            "SUCCEEDED",
        )
        agents_helper.pause_deploy(commons.REQUEST, self.envName, stageName, self.host)
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "RESTARTING",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "NOOP")
        self.assertTrue(response.get("deployGoal") is None)
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "RESTARTING",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "NOOP")
        self.assertTrue(response.get("deployGoal") is None)

        # verify agent state still on hold
        agents = agents_helper.get_agents_by_host(commons.REQUEST, self.host)
        self.assertEqual(len(agents), 1)
        self.assertEqual(agents[0].get("state"), "PAUSED_BY_USER")

    def test_agent_reset(self):
        self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "PRE_RESTART",
            "SUCCEEDED",
        )
        agents_helper.retry_deploy(commons.REQUEST, self.envName, stageName, self.host)
        # ping will start from beginning
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "RESTARTING",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "DEPLOY")
        goal = response["deployGoal"]
        self.assertEqual(goal["deployStage"], "PRE_DOWNLOAD")
        # and next ping will not reset anymore
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "PRE_DOWNLOAD",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "DEPLOY")
        goal = response["deployGoal"]
        self.assertEqual(goal["deployStage"], "DOWNLOADING")

        # verify agent state not RESET anymore
        agents = agents_helper.get_agents_by_host(commons.REQUEST, self.host)
        self.assertEqual(len(agents), 1)
        self.assertEqual(agents[0].get("state"), "NORMAL")

    def test_agents_reset(self):
        self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "PRE_RESTART",
            "TOO_MANY_RETRY",
            error_code=1,
            error_message="FOO",
        )
        agents = agents_helper.get_agents_by_host(commons.REQUEST, self.host)
        self.assertEqual(len(agents), 1)
        self.assertEqual(agents[0].get("state"), "PAUSED_BY_SYSTEM")
        self.assertEqual(agents[0].get("lastErrno"), 1)
        self.assertEqual(
            agents_helper.get_agent_error(
                commons.REQUEST, self.envName, stageName, self.host
            ),
            "FOO",
        )

        # reset all the failed agents
        agents_helper.reset_failed_agents(
            commons.REQUEST, self.envName, stageName, self.deploy["id"]
        )
        # ping will start from beginning
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "RESTARTING",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "DEPLOY")
        goal = response["deployGoal"]
        self.assertEqual(goal["deployStage"], "PRE_DOWNLOAD")
        # and next ping will not reset anymore
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "PRE_DOWNLOAD",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "DEPLOY")
        goal = response["deployGoal"]
        self.assertEqual(goal["deployStage"], "DOWNLOADING")

    def test_ping_serving_build(self):
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "SERVING_BUILD",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "NOOP")
        self.assertTrue(response.get("deployGoal") is None)

    def test_ping_empty(self):
        response = self.ping(self.host, self.groups)
        self.assertEqual(response["opCode"], "DEPLOY")
        goal = response["deployGoal"]
        self.assertEqual(goal["envId"], self.env["id"])
        self.assertEqual(goal["deployId"], self.deploy["id"])
        self.assertEqual(goal["deployStage"], "PRE_DOWNLOAD")
        self.assertEqual(goal["scriptVariables"]["s-c-n"], "s-c-v")
        self.assertEqual(goal["agentConfigs"]["a-c-n"], "a-c-v")

    def test_ping_next(self):
        response = self.ping(
            self.host,
            self.groups,
            self.env["id"],
            self.deploy["id"],
            "PRE_DOWNLOAD",
            "SUCCEEDED",
        )
        self.assertEqual(response["opCode"], "DEPLOY")
        goal = response["deployGoal"]
        self.assertEqual(goal["envId"], self.env["id"])
        self.assertEqual(goal["deployId"], self.deploy["id"])
        self.assertEqual(goal["deployStage"], "DOWNLOADING")
        self.assertEqual(goal["build"]["commit"], self.commit)
        self.assertEqual(goal["agentConfigs"]["a-c-n"], "a-c-v")


if __name__ == "__main__":
    unittest.main()
