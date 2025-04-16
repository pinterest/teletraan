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
import unittest
import time
import commons
import sys

builds_helper = commons.get_build_helper()
environs_helper = commons.get_environ_helper()
systems_helper = commons.get_system_helper()
deploys_helper = commons.get_deploy_helper()

CANARY = "canary"
PROD = "prod"


class TestAutoDeploy(unittest.TestCase):
    def setUp(self):
        self.host = "test-cd-host-" + commons.gen_random_num()
        self.envName = "test-cd-env-" + commons.gen_random_num()
        self.env_canary = commons.create_env(self.envName, CANARY)
        self.env_prod = commons.create_env(self.envName, PROD)
        self.commit = commons.gen_random_num(32)
        self.build = commons.publish_build(self.envName, commit=self.commit)

    def tearDown(self):
        environs_helper.delete_env(commons.REQUEST, self.envName, CANARY)
        environs_helper.delete_env(commons.REQUEST, self.envName, PROD)
        builds_helper.delete_build(commons.REQUEST, self.build["id"])

    def _fail_deploy(self, deploy, stage=PROD):
        pingRequest = {}
        pingRequest["hostId"] = self.host
        pingRequest["hostName"] = self.host
        pingRequest["hostIp"] = "8.8.8.8"
        report = {}
        report["envId"] = self.env_prod["id"]
        report["deployId"] = deploy["id"]
        report["deployStage"] = "RESTARTING"
        report["agentStatus"] = "TOO_MANY_RETRY"
        pingRequest["reports"] = [report]
        systems_helper.ping(commons.REQUEST, pingRequest)

    def _assertDeploy(self, stage, expect_commit):
        count = 0
        while count < 300:
            env = environs_helper.get_env_by_stage(commons.REQUEST, self.envName, stage)
            deployId = env.get("deployId")
            if deployId:
                deploy = deploys_helper.get(commons.REQUEST, env["deployId"])
                build = builds_helper.get_build(commons.REQUEST, deploy["buildId"])
                if build["commit"] == expect_commit:
                    return
            print(("."), end=" ")
            sys.stdout.flush()
            time.sleep(1)
            count = count + 1
        self.fail("Timed out when wait for deploy for %s to happen" % expect_commit)

    def testBuildToCanaryToProd(self):
        data = {}
        data["type"] = "AUTO"
        data["predStage"] = "BUILD"
        environs_helper.update_env_promotes_config(
            commons.REQUEST, self.envName, CANARY, data=data
        )
        data["predStage"] = CANARY
        environs_helper.update_env_promotes_config(
            commons.REQUEST, self.envName, PROD, data=data
        )
        self._assertDeploy(PROD, self.commit)
        deployId = environs_helper.get_env_by_stage(
            commons.REQUEST, self.envName, CANARY
        )["deployId"]
        deploys_helper.delete(commons.REQUEST, deployId)
        deployId = environs_helper.get_env_by_stage(
            commons.REQUEST, self.envName, PROD
        )["deployId"]
        deploys_helper.delete(commons.REQUEST, deployId)

    def testDisablePolicyAuto(self):
        data = {}
        data["type"] = "AUTO"
        data["disablePolicy"] = "AUTO"
        environs_helper.update_env_promotes_config(
            commons.REQUEST, self.envName, PROD, data=data
        )
        deploy = deploys_helper.deploy(
            commons.REQUEST, self.envName, PROD, self.build["id"]
        )
        promote_config = environs_helper.get_env_promotes_config(
            commons.REQUEST, self.envName, PROD
        )
        self.assertEqual(promote_config["type"], "MANUAL")
        deploys_helper.delete(commons.REQUEST, deploy["id"])

    def testDisablePolicyManual(self):
        data = {}
        data["type"] = "AUTO"
        data["disablePolicy"] = "MANUAL"
        environs_helper.update_env_promotes_config(
            commons.REQUEST, self.envName, PROD, data=data
        )
        deploy = deploys_helper.deploy(
            commons.REQUEST, self.envName, PROD, self.build["id"]
        )
        promote_config = environs_helper.get_env_promotes_config(
            commons.REQUEST, self.envName, PROD
        )
        self.assertEqual(promote_config["type"], "AUTO")
        deploys_helper.delete(commons.REQUEST, deploy["id"])

    def _assertState(self, stage, expect_state):
        count = 0
        while count < 150:
            promote_config = environs_helper.get_env_promotes_config(
                commons.REQUEST, self.envName, stage
            )
            if promote_config["type"] == expect_state:
                return
            print(("."), end=" ")
            sys.stdout.flush()
            time.sleep(1)
            count = count + 1
        self.fail("Timed out when wait for promote state to be %s" % expect_state)

    def testFailPolicyContinue(self):
        deploy = deploys_helper.deploy(
            commons.REQUEST, self.envName, PROD, self.build["id"]
        )
        environs_helper.update_env_capacity(
            commons.REQUEST, self.envName, PROD, capacity_type="HOST", data=[self.host]
        )
        self._fail_deploy(deploy)
        # make deploy fail happens faster
        deploys_helper.update_progress(commons.REQUEST, self.envName, PROD)
        data = {}
        data["type"] = "AUTO"
        data["disablePolicy"] = "MANUAL"
        data["predStage"] = CANARY
        data["failPolicy"] = "CONTINUE"
        environs_helper.update_env_promotes_config(
            commons.REQUEST, self.envName, PROD, data=data
        )
        promote_config = environs_helper.get_env_promotes_config(
            commons.REQUEST, self.envName, PROD
        )
        self.assertEqual(promote_config["type"], "AUTO")
        deploys_helper.delete(commons.REQUEST, deploy["id"])
        # This so that we can delete the env
        environs_helper.update_env_capacity(
            commons.REQUEST, self.envName, PROD, capacity_type="HOST", data=[]
        )

    def testFailPolicyDisable(self):
        environs_helper.update_env_capacity(
            commons.REQUEST, self.envName, PROD, capacity_type="HOST", data=[self.host]
        )
        deploy = deploys_helper.deploy(
            commons.REQUEST, self.envName, PROD, self.build["id"]
        )
        self._fail_deploy(deploy)
        # make deploy fail happens faster
        deploys_helper.update_progress(commons.REQUEST, self.envName, PROD)
        data = {}
        data["type"] = "AUTO"
        data["disablePolicy"] = "MANUAL"
        data["predStage"] = CANARY
        data["failPolicy"] = "DISABLE"
        environs_helper.update_env_promotes_config(
            commons.REQUEST, self.envName, PROD, data=data
        )
        self._assertState(PROD, "MANUAL")
        deploys_helper.delete(commons.REQUEST, deploy["id"])
        # This so that we can delete the env
        environs_helper.update_env_capacity(
            commons.REQUEST, self.envName, PROD, capacity_type="HOST", data=[]
        )

    def testFailPolicyROLLBACK(self):
        commit_0 = commons.gen_random_num(32)
        build_0 = commons.publish_build(self.envName, commit=commit_0)
        deploy_0 = deploys_helper.deploy(
            commons.REQUEST, self.envName, PROD, build_0["id"]
        )
        deploys_helper.update_progress(commons.REQUEST, self.envName, PROD)
        environs_helper.update_env_capacity(
            commons.REQUEST, self.envName, PROD, capacity_type="HOST", data=[self.host]
        )
        deploy = deploys_helper.deploy(
            commons.REQUEST, self.envName, PROD, self.build["id"]
        )
        self._fail_deploy(deploy)
        deploys_helper.update_progress(commons.REQUEST, self.envName, PROD)

        data = {}
        data["type"] = "AUTO"
        data["disablePolicy"] = "MANUAL"
        data["predStage"] = CANARY
        data["failPolicy"] = "ROLLBACK"
        environs_helper.update_env_promotes_config(
            commons.REQUEST, self.envName, PROD, data=data
        )

        self._assertState(PROD, "MANUAL")
        new_env = environs_helper.get_env_by_stage(commons.REQUEST, self.envName, PROD)
        new_deploy = deploys_helper.get(commons.REQUEST, new_env["deployId"])
        new_build = builds_helper.get_build(commons.REQUEST, new_deploy["buildId"])
        self.assertEqual(new_build["commit"], commit_0)

        builds_helper.delete_build(commons.REQUEST, build_0["id"])
        deploys_helper.delete(commons.REQUEST, deploy_0["id"])
        deploys_helper.delete(commons.REQUEST, deploy["id"])
        deploys_helper.delete(commons.REQUEST, new_deploy["id"])
        # This so that we can delete the env
        environs_helper.update_env_capacity(
            commons.REQUEST, self.envName, PROD, capacity_type="HOST", data=[]
        )


if __name__ == "__main__":
    unittest.main()
