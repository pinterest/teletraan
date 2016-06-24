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

import os
import unittest
import mock
import tests

from deployd.agent import DeployAgent
from deployd.client.serverless_client import ServerlessClient
from deployd.common.utils import ensure_dirs
from deployd.common.types import DeployReport, DeployStatus, OpCode, DeployStage, AgentStatus
from deployd.types.deploy_goal import DeployGoal
from deployd.types.ping_report import PingReport
from deployd.types.ping_response import PingResponse


class TestServerlessClient(tests.TestCase):

    def setUp(self):
        self.env_name = "test"
        self.stage = "prod"
        self.env_id = "12343434"
        self.build = '{"commit":"c5a7f50453fa70fefa41dc5b75e9b053fc5bba4b","id":"S2dUHIrFSMyDzdwO-6mgeA_c81d6b3","branch":"master","artifactUrl":"https://deployrepo.pinadmin.com/pinboard/pinboard-c5a7f50.tar.gz","repo":"P","name": "pinboard"}' 
        self.script_variables = '{"IS_DOCKER": "True"}'
        self.client = ServerlessClient(env_name=self.env_name, stage=self.stage, build=self.build,
                                       script_variables=self.script_variables) 

    def _new_report(self):
        report = PingReport()
        report.envName = self.env_name
        report.stageName = self.stage
        report.erroCode = 0
        report.envId  = self.env_id
        report.deployStage = None
        report.status = AgentStatus.SUCCEEDED
        return report
              
    def test_deploy_stage_trnasition(self):
        report = self._new_report()
        deploy_status = DeployStatus()
        deploy_status.report = report
        env_status = {self.env_name : deploy_status}

        deployStages = ['PRE_DOWNLOAD', 'DOWNLOADING', 'POST_DOWNLOAD', 'STAGING', 'PRE_RESTART', 'RESTARTING', 'POST_RESTART', 'SERVING_BUILD']

        for i in range(0, len(deployStages)):
            response = self.client.send_reports(env_status) 
            self.assertEqual(response.opCode, "DEPLOY")
            self.assertEqual(response.deployGoal.deployStage, deployStages[i])
            report.deployStage = response.deployGoal.deployStage
            report.deployId = response.deployGoal.deployId

        # test ending case
        response = self.client.send_reports(env_status)
        self.assertEqual(response.deployGoal, None)
 
    def test_errorcode_stop_deployment(self):
        report = self._new_report()
        deploy_status = DeployStatus()
        deploy_status.report = report
        env_status = {self.env_name : deploy_status}

        # first try is allowed.
        report.errorCode = 123 
        response = self.client.send_reports(env_status)
        report.deployStage = response.deployGoal.deployStage
        report.deployId = response.deployGoal.deployId

        response = self.client.send_reports(env_status)
        self.assertEqual(response, None)

    def test_unknow_status_cause_retry(self):
        report = self._new_report()
        deploy_status = DeployStatus()
        deploy_status.report = report
        env_status = {self.env_name : deploy_status}

        report.status = AgentStatus.UNKNOWN 
        response = self.client.send_reports(env_status)
        report.deployStage = response.deployGoal.deployStage
        report.deployId = response.deployGoal.deployId

        response = self.client.send_reports(env_status)
        self.assertEqual(response.deployGoal.deployStage, 'PRE_DOWNLOAD')
 

if __name__ == '__main__':
    unittest.main()
