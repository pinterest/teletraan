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

import logging
import json
from typing import Optional
import uuid

from deployd.client.base_client import BaseClient
from deployd.common import utils
from deployd.common.types import AgentStatus
from deployd.types.deploy_stage import DeployStage
from deployd.types.opcode import OperationCode
from deployd.types.ping_response import PingResponse

log: logging.Logger = logging.getLogger(__name__)

_DEPLOY_STAGE_TRANSITIONS = dict([(i, i+1) for i in range(DeployStage.PRE_DOWNLOAD, DeployStage.SERVING_BUILD)])


class ServerlessClient(BaseClient):
    """This client is supposed to be used when teletraan sever is not available, i.e
    when agent runs in serverless job. The client will simulate teletraan service by
    guiding the agent through one complete deployment process.

    A deployment process will go through following pipeline
      PRE_DOWNLOAD->DOWNLOADING->POST_DOWNLOAD->STAGING->PRE_RESTART
          ->RESTARTING->POST_RESTART->SERVING_BUILD
    """
    def __init__(self, env_name, stage, build, script_variables, deploy_stage: Optional[DeployStage] = None) -> None:
        """build contains build information in json format. It contains information defined in types/build.py.
        """
        self._env_name: str = utils.check_not_none(env_name, 'env_name can not be None')
        self._stage: str = utils.check_not_none(stage, 'stage name can not be None')
        self._build: dict[str, str] = json.loads(utils.check_not_none(build, 'build can not be None'))
        
        self._script_variables: dict[str, str] = json.loads(utils.check_not_none(script_variables, 'script_variables can not be None'))
        self._deploy_id: str = uuid.uuid4().hex
        self._deploy_stage: DeployStage = deploy_stage if deploy_stage is not None else DeployStage.PRE_DOWNLOAD

    def send_reports(self, env_reports=None) -> Optional[PingResponse]:
        reports: list = [status.report for status in env_reports.values()]
        for report in reports:
            if report.envName != self._env_name:
                continue
            self._env_id = report.envId 
            ping_response: Optional[PingResponse] = self._create_response(report)
            log.info(f"{str(report)} {str(ping_response)}")
            return ping_response

        # env_status file might be corrupted or first deploy.
        self._env_id = uuid.uuid4().hex
        return self._create_response(None)

    def _create_response(self, report) -> Optional[PingResponse]:
        # check if this is the first step
        if report is None or report.deployId is None or report.deployId != self._deploy_id:
            # first report from agent, start first deploy stage.
            return self._new_response_value(numeric_deploy_stage=self._deploy_stage)
        if report.errorCode != 0:
            # terminate the deployment.
            return None
        numeric_deploy_stage = DeployStage._NAMES_TO_VALUES[report.deployStage] 
        if report.status == AgentStatus.SUCCEEDED:
            # check if this is the last deploy stage.
            if numeric_deploy_stage == DeployStage.SERVING_BUILD:
                return PingResponse({'opCode': OperationCode.NOOP})

            # move to next deploy stage
            next_deploy_stage = _DEPLOY_STAGE_TRANSITIONS.get(numeric_deploy_stage)
            return self._new_response_value(next_deploy_stage)

        if report.status == AgentStatus.UNKNOWN:
            # retry current stage
            return self._new_response_value(numeric_deploy_stage)

        # terminate deployment
        return None

    def _new_response_value(self, numeric_deploy_stage) -> PingResponse:
        value= {'opCode': OperationCode.DEPLOY,
                'deployGoal': {'deployId': self._deploy_id,
                               'envId': self._env_id,
                               'envName': self._env_name,
                               'stageName': self._stage,
                               'build': self._build,
                               'deployStage': numeric_deploy_stage}}
        if numeric_deploy_stage == DeployStage.PRE_DOWNLOAD:
            value['deployGoal']['scriptVariables'] = self._script_variables
        return PingResponse(jsonValue=value)
