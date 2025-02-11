# Copyright 2022 Pinterest, Inc.
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

from deployd.types.deploy_goal import DeployGoal
from deployd.types.build import Build
import unittest


class TestDeployGoal(unittest.TestCase):
    deployId = "deployId"
    envId = "envId"
    envName = "envName"
    stageName = "stageName"
    deployStage = "deployStage"
    build = "build"
    deployAlias = "deployAlias"
    config = "agentConfigs"
    j_agentConfigs = "agentConfigs"
    scriptVariables = "scriptVariables"
    firstDeploy = "firstDeploy"
    isDocker = "isDocker"

    def test__no_values(self):
        deploy_goal = DeployGoal(jsonValue=None)
        self.assertIsNone(deploy_goal.deployId)
        self.assertIsNone(deploy_goal.envId)
        self.assertIsNone(deploy_goal.envName)
        self.assertIsNone(deploy_goal.stageName)
        self.assertIsNone(deploy_goal.deployStage)
        self.assertIsNone(deploy_goal.deployAlias)
        self.assertIsNone(deploy_goal.config)
        self.assertIsNone(deploy_goal.scriptVariables)
        self.assertIsNone(deploy_goal.firstDeploy)
        self.assertIsNone(deploy_goal.isDocker)

    def test__values(self):
        data = {
            self.deployId: self.deployId,
            self.envId: self.envId,
            self.envName: self.envName,
            self.stageName: self.stageName,
            self.deployStage: self.deployStage,
            self.deployAlias: self.deployAlias,
            self.j_agentConfigs: self.config,
            self.scriptVariables: self.scriptVariables,
            self.firstDeploy: self.firstDeploy,
            self.isDocker: self.isDocker,
        }
        deploy_goal = DeployGoal(jsonValue=data)
        self.assertEqual(self.deployId, deploy_goal.deployId)
        self.assertEqual(self.envId, deploy_goal.envId)
        self.assertEqual(self.envName, deploy_goal.envName)
        self.assertEqual(self.stageName, deploy_goal.stageName)
        self.assertEqual(self.deployStage, deploy_goal.deployStage)
        self.assertEqual(self.deployAlias, deploy_goal.deployAlias)
        self.assertEqual(self.config, deploy_goal.config)
        self.assertEqual(self.scriptVariables, deploy_goal.scriptVariables)
        self.assertEqual(self.firstDeploy, deploy_goal.firstDeploy)
        self.assertEqual(self.isDocker, deploy_goal.isDocker)

    def test__build(self):
        data = {self.build: {"id": 1}}
        deploy_goal = DeployGoal(jsonValue=data)
        self.assertIsInstance(deploy_goal.build, Build)

    def test__deploy_stage(self):
        data = {self.deployStage: 1}
        deploy_goal = DeployGoal(jsonValue=data)
        self.assertIsInstance(deploy_goal.deployStage, str)

        data = {self.deployStage: self.deployStage}
        deploy_goal = DeployGoal(jsonValue=data)
        self.assertIsInstance(deploy_goal.deployStage, str)

    def test____eq__(self):
        data_1 = {
            self.deployId: self.deployId,
            self.envId: self.envId,
            self.envName: self.envName,
            self.stageName: self.stageName,
            self.deployStage: self.deployStage,
            self.deployAlias: self.deployAlias,
            self.j_agentConfigs: self.config,
            self.scriptVariables: self.scriptVariables,
            self.firstDeploy: self.firstDeploy,
            self.isDocker: self.isDocker,
        }
        deploy_goal_1 = DeployGoal(jsonValue=data_1)
        data_2 = {
            self.deployId: self.deployId,
            self.envId: self.envId,
            self.envName: self.envName,
            self.stageName: self.stageName,
            self.deployStage: self.deployStage,
            self.deployAlias: self.deployAlias,
            self.j_agentConfigs: self.config,
            self.scriptVariables: self.scriptVariables,
            self.firstDeploy: self.firstDeploy,
            self.isDocker: self.isDocker,
        }
        deploy_goal_2 = DeployGoal(jsonValue=data_2)
        self.assertEqual(deploy_goal_1, deploy_goal_2)
        self.assertTrue(DeployGoal(jsonValue=None) == DeployGoal(jsonValue=None))
        self.assertFalse(DeployGoal(jsonValue=None) is None)

    def test____ne__(self):
        data = {
            self.deployId: self.deployId,
            self.envId: self.envId,
            self.envName: self.envName,
            self.stageName: self.stageName,
            self.deployStage: self.deployStage,
            self.deployAlias: self.deployAlias,
            self.j_agentConfigs: self.config,
            self.scriptVariables: self.scriptVariables,
            self.firstDeploy: self.firstDeploy,
            self.isDocker: self.isDocker,
        }
        deploy_goal = DeployGoal(jsonValue=data)
        other = "other"
        self.assertNotEqual(deploy_goal, DeployGoal(jsonValue={self.deployId: other}))
        self.assertNotEqual(deploy_goal, DeployGoal(jsonValue={self.envId: other}))
        self.assertNotEqual(deploy_goal, DeployGoal(jsonValue={self.envName: other}))
        self.assertNotEqual(deploy_goal, DeployGoal(jsonValue={self.stageName: other}))
        self.assertNotEqual(
            deploy_goal, DeployGoal(jsonValue={self.deployStage: other})
        )
        self.assertNotEqual(
            deploy_goal, DeployGoal(jsonValue={self.deployAlias: other})
        )
        self.assertNotEqual(
            deploy_goal, DeployGoal(jsonValue={self.j_agentConfigs: other})
        )
        self.assertNotEqual(
            deploy_goal, DeployGoal(jsonValue={self.scriptVariables: other})
        )
        self.assertNotEqual(
            deploy_goal, DeployGoal(jsonValue={self.firstDeploy: other})
        )
        self.assertNotEqual(deploy_goal, DeployGoal(jsonValue={self.isDocker: other}))
        self.assertTrue(DeployGoal(jsonValue=None) is not None)
        self.assertFalse(DeployGoal(jsonValue=None) == "")
