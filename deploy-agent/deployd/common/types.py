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

from deployd.types.ping_report import PingReport
from deployd.types.opcode import OperationCode


class AgentStatus(object):
    SUCCEEDED = 'SUCCEEDED'
    UNKNOWN = 'UNKNOWN'
    AGENT_FAILED = 'AGENT_FAILED'
    RETRYABLE_AGENT_FAILED = 'RETRYABLE_AGENT_FAILED'
    # TODO This is not currently used at all, consider to remove it
    SCRIPT_FAILED = 'SCRIPT_FAILED'
    # TODO Agent execution is aborted manually on the host, server does not handle it right now
    ABORTED_BY_SERVICE = 'ABORTED_BY_SERVICE'
    SCRIPT_TIMEOUT = 'SCRIPT_TIMEOUT'
    TOO_MANY_RETRY = 'TOO_MANY_RETRY'
    RUNTIME_MISMATCH = 'RUNTIME_MISMATCH'
    ABORTED_BY_SERVER = 'ABORTED_BY_SERVER'


class DeployStage(object):
    UNKNOWN = 'UNKNOWN'
    PRE_DOWNLOAD = 'PRE_DOWNLOAD'
    DOWNLOADING = 'DOWNLOADING'
    POST_DOWNLOAD = 'POST_DOWNLOAD'
    STAGING = 'STAGING'
    PRE_RESTART = 'PRE_RESTART'
    RESTARTING = 'RESTARTING'
    POST_RESTART = 'POST_RESTART'
    SERVING_BUILD = 'SERVING_BUILD'
    STOPPING = 'STOPPING'
    STOPPED = 'STOPPED'


class DeployType(object):
    REGULAR = 'REGULAR'
    HOTFIX = 'HOTFIX'
    ROLLBACK = 'ROLLBACK'
    RESTART = 'RESTART'
    STOP = 'STOP'


class OpCode(object):
    NOOP = 'NOOP'
    DEPLOY = 'DEPLOY'
    UPDATE = 'UPDATE'
    RESTART = 'RESTART'
    DELETE = 'DELETE'
    TERMINATE = 'TERMINATE'
    WAIT = 'WAIT'
    ROLLBACK = 'ROLLBACK'
    STOP = 'STOP'


class DeployReport(object):

    def __init__(self, status_code=AgentStatus.UNKNOWN,
                 error_code=0, output_msg=None, retry_times=0):
        self.status_code = status_code
        self.error_code = error_code
        self.output_msg = output_msg
        self.retry_times = retry_times


class PingStatus(object):
    PLAN_NO_CHANGE = 0  # the deploy plan is not changed
    PLAN_CHANGED = 1  # the deploy goal is changed
    PING_FAILED = 2  # failed to contact the server


class BuildInfo(object):

    def __init__(self, commit, build_url, build_id, build_name=None,
                 build_repo=None, build_branch=None):
        self.build_commit = commit
        self.build_url = build_url
        self.build_id = build_id
        self.build_name = build_name
        self.build_repo = build_repo
        self.build_branch = build_branch

    def __eq__(self, other):
        if other:
            return self.__dict__ == other.__dict__
        else:
            return False


class DeployStatus(object):
    report = None
    build_info = None
    runtime_config = None
    first_deploy = None
    op_code = OpCode.NOOP
    script_variables = None
    is_docker = None

    def __init__(self, response=None, json_value=None):
        if response:
            self.update_by_response(response)
        elif json_value:
            self.load_from_json(json_value)

    def __eq__(self, other):
        return self.build_info == other.build_info and \
            self.report == other.report and \
            self.runtime_config == other.runtime_config and \
            self.op_code == other.op_code

    # update the deploy status by the current ping response from teletraan server
    def update_by_response(self, response):
        deploy_goal = response.deployGoal
        self.op_code = response.opCode
        if not self.report:
            self.report = PingReport()

        self.report.envId = deploy_goal.envId
        self.report.deployId = deploy_goal.deployId
        self.report.deployStage = deploy_goal.deployStage
        self.report.deployAlias = deploy_goal.deployAlias
        self.report.envName = deploy_goal.envName
        self.report.status = AgentStatus.UNKNOWN
        self.report.stageName = deploy_goal.stageName
        self.first_deploy = deploy_goal.firstDeploy
        self.is_docker = deploy_goal.isDocker

        if deploy_goal.build:
            build = deploy_goal.build
            self.build_info = BuildInfo(commit=build.scmCommit,
                                        build_url=build.artifactUrl,
                                        build_id=build.buildId,
                                        build_name=build.buildName,
                                        build_repo=build.scmRepo,
                                        build_branch=build.scmBranch)
        if deploy_goal.scriptVariables:
            self.script_variables = deploy_goal.scriptVariables

        # advanced per deploy step configs
        if deploy_goal.config:
            self.runtime_config = dict(deploy_goal.config)

    # update the env status with current deploy report
    def update_by_deploy_report(self, deploy_report):
        # aborted by server happens when
        if deploy_report.status_code == AgentStatus.ABORTED_BY_SERVER:
            self.report.status = AgentStatus.UNKNOWN
        else:
            self.report.status = deploy_report.status_code
        self.report.errorCode = deploy_report.error_code
        if self.report.status != AgentStatus.SUCCEEDED:
            self.report.errorMessage = deploy_report.output_msg
        else:
            self.report.errorMessage = None
        self.report.failCount = deploy_report.retry_times

    def load_from_json(self, json_value):
        report = json_value.get('report')
        build_info = json_value.get('build_info')
        if report:
            self.report = PingReport(jsonValue=report)
        if build_info:
            self.build_info = BuildInfo(commit=build_info.get('build_commit'),
                                        build_url=build_info.get('build_url'),
                                        build_id=build_info.get('build_id'),
                                        build_name=build_info.get('build_name'),
                                        build_repo=build_info.get('build_repo'),
                                        build_branch=build_info.get('build_branch'))

        self.runtime_config = json_value.get('runtime_config')
        op_code = json_value.get('op_code', OpCode.NOOP)
        if isinstance(op_code, int):
            self.op_code = OperationCode._VALUES_TO_NAMES[op_code]
        else:
            self.op_code = op_code

    def to_json(self):
        json = {}
        for key, value in self.__dict__.items():
            if type(value) is dict:
                json[key] = {k: v for k, v in value.items()}
            elif value:
                if hasattr(value, "__dict__"):
                    json[key] = value.__dict__
                else:
                    json[key] = value
        return json

    def __str__(self):
        return str(self.to_json())


# TODO prefer get it from server ( STEPS, PRE_STAGE_STEPS etc.)
PRE_STAGE_STEPS = ('PRE_DOWNLOAD', 'DOWNLOADING', 'POST_DOWNLOAD', 'STAGING')
