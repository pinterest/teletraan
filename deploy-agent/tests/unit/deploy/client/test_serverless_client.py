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

from typing import Optional
import unittest
from tests import TestCase

from deployd.client.serverless_client import ServerlessClient
from deployd.common.types import DeployStage, DeployStatus, AgentStatus
from deployd.types.ping_report import PingReport
from deployd.types.ping_response import PingResponse


class TestServerlessClient(TestCase):

    def setUp(self) -> None:
        self.env_name = "test"
        self.stage = "prod"
        self.env_id = "12343434"
        self.build = '{"commit":"c5a7f50453fa70fefa41dc5b75e9b053fc5bba4b","id":"S2dUHIrFSMyDzdwO-6mgeA_c81d6b3","branch":"master","artifactUrl":"https://deployrepo.pinadmin.com/pinboard/pinboard-c5a7f50.tar.gz","repo":"P","name": "pinboard"}' 
        self.script_variables = '{"IS_DOCKER": "True"}'
        self.client = ServerlessClient(env_name=self.env_name, stage=self.stage, build=self.build,
                                       script_variables=self.script_variables) 

    def _new_report(self) -> PingReport:
        report = PingReport()
        report.envName = self.env_name
        report.stageName = self.stage
        report.erroCode = 0
        report.envId  = self.env_id
        report.deployStage = None
        report.status = AgentStatus.SUCCEEDED
        return report
              
    def test_deploy_stage_transition(self) -> None:
        report: PingReport = self._new_report()
        deploy_status = DeployStatus()
        deploy_status.report = report
        env_status: dict[str, DeployStatus] = {self.env_name : deploy_status}

        deployStages: list[str] = ['PRE_DOWNLOAD', 'DOWNLOADING', 'POST_DOWNLOAD', 'STAGING', 'PRE_RESTART', 'RESTARTING', 'POST_RESTART', 'SERVING_BUILD']

        for i in range(0, len(deployStages)):
            response: Optional[PingReport] = self.client.send_reports(env_status) 
            self.assertEqual(response.opCode, "DEPLOY")
            self.assertEqual(response.deployGoal.deployStage, deployStages[i])
            report.deployStage = response.deployGoal.deployStage
            report.deployId = response.deployGoal.deployId

        # test ending case
        response = self.client.send_reports(env_status)
        self.assertEqual(response.deployGoal, None)
 
    def test_run_with_defined_deploy_stage(self) -> None:
        self.client = ServerlessClient(env_name=self.env_name, stage=self.stage, build=self.build,
                                       script_variables=self.script_variables, deploy_stage=DeployStage.PRE_RESTART)
        report: PingReport = self._new_report()
        report.deployId = None
        deploy_status = DeployStatus()
        deploy_status.report = report
        env_status: dict[str, DeployStatus] = {self.env_name : deploy_status}

        response: Optional[PingResponse] = self.client.send_reports(env_status)
        self.assertEqual(response.opCode, "DEPLOY")
        self.assertEqual(response.deployGoal.deployStage, 'PRE_RESTART')
 
    def test_errorcode_stop_deployment(self):
        report: PingReport = self._new_report()
        deploy_status = DeployStatus()
        deploy_status.report = report
        env_status: dict[str, DeployStatus] = {self.env_name : deploy_status}

        # first try is allowed.
        report.errorCode = 123 
        response: Optional[PingResponse] = self.client.send_reports(env_status)
        report.deployStage = response.deployGoal.deployStage
        report.deployId = response.deployGoal.deployId

        response = self.client.send_reports(env_status)
        self.assertEqual(response, None)

    def test_unknow_status_cause_retry(self):
        report: PingReport = self._new_report()
        deploy_status = DeployStatus()
        deploy_status.report = report
        env_status: dict[str, DeployStatus] = {self.env_name : deploy_status}

        report.status = AgentStatus.UNKNOWN 
        response: Optional[PingResponse] = self.client.send_reports(env_status)
        report.deployStage = response.deployGoal.deployStage
        report.deployId = response.deployGoal.deployId

        response = self.client.send_reports(env_status)
        self.assertEqual(response.deployGoal.deployStage, 'PRE_DOWNLOAD')
    
    def test_create_response_last_deploy_stage(self):
        report = self._new_report()
        report.deployId = self.client._deploy_id
        report.status = "SUCCEEDED"
        report.deployStage = "SERVING_BUILD" 
        response: PingResponse = self.client._create_response(report)
        self.assertEqual(response.opCode, "NOOP")
 

if __name__ == '__main__':
    unittest.main()
