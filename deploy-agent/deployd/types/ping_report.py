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

from deployd.types.deploy_stage import DeployStage
from deployd.types.agent_status import AgentStatus


class PingReport(object):

    def __init__(self, jsonValue=None):
        self.deployId = None
        self.envId = None
        self.envName = None
        self.stageName = None
        self.deployStage = None
        self.status = None
        self.errorCode = 0
        self.errorMessage = None
        self.failCount = 0
        self.extraInfo = None
        self.deployAlias = None

        if jsonValue:
            self.deployId = jsonValue.get('deployId')
            self.envId = jsonValue.get('envId')
            if isinstance(jsonValue.get('deployStage'), int):
                self.deployStage = DeployStage._VALUES_TO_NAMES[jsonValue.get('deployStage')]
            else:
                self.deployStage = jsonValue.get('deployStage')

            if isinstance(jsonValue.get('status'), int):
                self.status = AgentStatus._VALUES_TO_NAMES[jsonValue.get('status')]
            else:
                self.status = jsonValue.get('status')

            self.envName = jsonValue.get('envName')
            self.stageName = jsonValue.get('stageName')
            self.errorCode = jsonValue.get('errorCode')
            self.errorMessage = jsonValue.get('errorMessage')
            self.failCount = jsonValue.get('failCount')
            self.extraInfo = jsonValue.get('extraInfo')
            self.deployAlias = jsonValue.get('deployAlias')

    def __str__(self):
        return "PingReport(deployId={}, envId={}, deployStage={}, status={}, " \
               "errorCode={}), errorMessage={}, failCount={}, extraInfo={}, " \
               "deployAlias={})".format(self.deployId, self.envId, self.deployStage,
                                        self.status, self.errorCode, self.errorMessage,
                                        self.failCount, self.extraInfo, self.deployAlias,)
