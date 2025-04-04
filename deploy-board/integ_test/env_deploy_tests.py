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
import commons

builds_helper = commons.get_build_helper()
environs_helper = commons.get_environ_helper()
systems_helper = commons.get_system_helper()
deploys_helper = commons.get_deploy_helper()

CANARY = "canary"
PROD = "prod"


class TestDeploys(unittest.TestCase):
    envName = ""
    buildId1 = ""
    buildId2 = ""
    canaryEnvId = ""
    prodEnvId = ""

    @classmethod
    def setUpClass(cls):
        cls.envName = "test-deploy-" + commons.gen_random_num()
        data = {}
        data["envName"] = cls.envName
        data["stageName"] = CANARY
        data["acceptanceType"] = "MANUAL"
        cls.canaryEnvId = environs_helper.create_env(commons.REQUEST, data)["id"]

        data = {}
        data["envName"] = cls.envName
        data["stageName"] = PROD
        cls.prodEnvId = environs_helper.create_env(commons.REQUEST, data)["id"]

        cls.buildId1 = commons.publish_build(cls.envName)["id"]
        cls.buildId2 = commons.publish_build(cls.envName)["id"]

    @classmethod
    def tearDownClass(cls):
        environs_helper.delete_env(commons.REQUEST, cls.envName, CANARY)
        environs_helper.delete_env(commons.REQUEST, cls.envName, PROD)
        builds_helper.delete_build(commons.REQUEST, cls.buildId1)
        builds_helper.delete_build(commons.REQUEST, cls.buildId2)

    def testDeploys(self):
        # test regular deploy
        deploy1 = deploys_helper.deploy(
            commons.REQUEST, TestDeploys.envName, CANARY, TestDeploys.buildId1
        )
        self.assertEqual(deploy1["envId"], TestDeploys.canaryEnvId)
        self.assertEqual(deploy1["buildId"], TestDeploys.buildId1)
        self.assertEqual(deploy1["type"], "REGULAR")
        self.assertNotEqual(deploy1["acceptanceStatus"], "ACCEPTED")
        self.assertEqual(deploy1["successTotal"], 0)
        self.assertEqual(deploy1["failTotal"], 0)
        self.assertEqual(deploy1["total"], 0)

        deploy2 = deploys_helper.deploy(
            commons.REQUEST, TestDeploys.envName, CANARY, TestDeploys.buildId2
        )
        self.assertEqual(deploy2["buildId"], TestDeploys.buildId2)
        # test query
        envIds = [TestDeploys.canaryEnvId, commons.gen_random_num()]
        deployResult = deploys_helper.get_all(
            commons.REQUEST, envId=envIds, oldestFirst=True, pageIndex=1, pageSize=1
        )
        self.assertEqual(deployResult["total"], 2)
        # TODO why we need truncated at all?
        self.assertTrue(deployResult["truncated"])
        deploys = deployResult["deploys"]
        self.assertEqual(len(deploys), 1)
        self.assertEqual(deploy1["id"], deploys[0]["id"])

        # test pause and resume
        deploys_helper.pause(commons.REQUEST, TestDeploys.envName, CANARY)
        envState = environs_helper.get_env_by_stage(
            commons.REQUEST, TestDeploys.envName, CANARY
        )["envState"]
        self.assertEqual(envState, "PAUSED")
        deploys_helper.resume(commons.REQUEST, TestDeploys.envName, CANARY)
        envState = environs_helper.get_env_by_stage(
            commons.REQUEST, TestDeploys.envName, CANARY
        )["envState"]
        self.assertEqual(envState, "NORMAL")

        # test restart
        deploy3 = deploys_helper.restart(commons.REQUEST, TestDeploys.envName, CANARY)
        self.assertEqual(deploy3["buildId"], TestDeploys.buildId2)
        self.assertEqual(deploy3["type"], "RESTART")

        # test rollback
        deploy4 = deploys_helper.rollback(
            commons.REQUEST, TestDeploys.envName, CANARY, deploy1["id"]
        )
        self.assertEqual(deploy4["buildId"], TestDeploys.buildId1)
        self.assertEqual(deploy4["type"], "ROLLBACK")

        # test promote
        deploy5 = deploys_helper.promote(
            commons.REQUEST, TestDeploys.envName, PROD, deploy4["id"]
        )
        self.assertEqual(deploy5["envId"], TestDeploys.prodEnvId)
        self.assertEqual(deploy5["buildId"], TestDeploys.buildId1)
        self.assertEqual(deploy5["type"], "REGULAR")
        self.assertEqual(deploy5["successTotal"], 0)
        self.assertEqual(deploy5["failTotal"], 0)
        self.assertEqual(deploy5["total"], 0)

        # delete all the deploys
        deploys_helper.delete(commons.REQUEST, deploy1["id"])
        deploys_helper.delete(commons.REQUEST, deploy2["id"])
        deploys_helper.delete(commons.REQUEST, deploy3["id"])
        deploys_helper.delete(commons.REQUEST, deploy4["id"])
        deploys_helper.delete(commons.REQUEST, deploy5["id"])


if __name__ == "__main__":
    unittest.main()
