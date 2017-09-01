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

from deployd.types.build import Build
from deployd.types.deploy_stage import DeployStage


class DeployGoal(object):
    def __init__(self, jsonValue=None):
        self.deployId = None
        self.envId = None
        self.envName = None
        self.stageName = None
        self.deployStage = None
        self.build = None
        self.deployAlias = None
        self.config = None
        self.scriptVariables = None
        self.firstDeploy = None
        self.isDocker = None

        if jsonValue:
            self.deployId = jsonValue.get('deployId')
            self.envId = jsonValue.get('envId')
            self.envName = jsonValue.get('envName')
            self.stageName = jsonValue.get('stageName')
            # TODO: Only used for migration, should remove later
            if isinstance(jsonValue.get('deployStage'), int):
                self.deployStage = DeployStage._VALUES_TO_NAMES[jsonValue.get('deployStage')]
            else:
                self.deployStage = jsonValue.get('deployStage')

            if jsonValue.get('build'):
                self.build = Build(jsonValue=jsonValue.get('build'))

            self.deployAlias = jsonValue.get('deployAlias')
            self.config = jsonValue.get('agentConfigs')
            self.scriptVariables = jsonValue.get('scriptVariables')
            self.firstDeploy = jsonValue.get('firstDeploy')
            self.isDocker = jsonValue.get('isDocker')

    def __str__(self):
        return "DeployGoal(deployId={}, envId={}, envName={}, stageName={}, " \
               "deployStage={}, build={}, deployAlias={}, agentConfig={}," \
               "scriptVariables={}, firstDeploy={}, isDocker={})".format(
                self.deployId, self.envId, self.envName,
                self.stageName, self.deployStage,
                self.build, self.deployAlias,
                self.config, self.scriptVariables,
                self.firstDeploy, self.isDocker)
