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

import json
from deployd.types.deploy_stage import DeployStage
from deployd.types.agent_status import AgentStatus


class PingRequest(object):

    def __init__(self, hostId=None, hostName=None, hostIp=None, groups=None, reports=None):
        self.hostId = hostId
        self.hostName = hostName
        self.hostIp = hostIp
        self.groups = groups
        self.reports = reports

    def to_json(self):
        ping_requests = {}
        ping_requests["hostId"] = self.hostId
        ping_requests["hostName"] = self.hostName
        ping_requests["hostIp"] = self.hostIp
        if self.groups:
            ping_requests["groups"] = list(self.groups)
        ping_requests["reports"] = []
        for report in self.reports:
            ping_report = {}
            ping_report["deployId"] = report.deployId
            ping_report["envId"] = report.envId

            # TODO: Only used for migration, should remove later
            if isinstance(report.deployStage, int):
                ping_report["deployStage"] = DeployStage._VALUES_TO_NAMES[report.deployStage]
            else:
                ping_report["deployStage"] = report.deployStage

            if isinstance(report.status, int):
                ping_report["agentStatus"] = AgentStatus._VALUES_TO_NAMES[report.status]
            else:
                ping_report["agentStatus"] = report.status

            ping_report["errorCode"] = report.errorCode
            ping_report["errorMessage"] = report.errorMessage
            ping_report["failCount"] = report.failCount
            ping_report["deployAlias"] = report.deployAlias
            if report.extraInfo:
                ping_report["extraInfo"] = \
                    json.dumps(report.extraInfo, ensure_ascii=False).encode('utf8')
            ping_requests["reports"].append(ping_report)
        return ping_requests

    def __str__(self):
        return "PingRequest(hostId={}, hostName={}, hostIp={}, groups={}, " \
               "reports={})".format(self.hostId, self.hostName, self.hostIp, self.groups,
                                    ",".join(str(v) for v in self.reports))
