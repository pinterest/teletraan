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
"""Helper functions to help generate agents views
"""
from deploy_board.webapp.helpers import agents_helper
from .common import is_agent_failed
from .helpers import builds_helper, deploys_helper, environs_helper, environ_hosts_helper
from deploy_board.settings import IS_PINTEREST
import time
from collections import OrderedDict
from functools import cmp_to_key


# Constants used to distinguish the action to generate the host report
TOTAL_ALIVE_HOST_REPORT = "TOTAL_ALIVE_HOST_REPORT"
UNKNOWN_HOST_REPORT = "UNKNOWN_HOST_REPORT"
PROVISION_HOST_REPORT = "PROVISION_HOST_REPORT"
TOTAL_HOST_REPORT = "TOTAL_HOST_REPORT"
FAILED_HOST_REPORT = "FAILED_HOST_REPORT"
ALIVE_STAGE_HOST_REPORT = "ALIVE_STAGE_HOST_REPORT"

DEFAULT_STALE_THRESHOLD = 300

# Error Code
UNKNOWN_HOSTS_CODE = -1000
PROVISION_HOST_CODE = -1001


class AgentStatistics(object):
    def __init__(self, agent=None, isCurrent=False, isStale=False, isHostFailed=False):
        self.agent = agent
        self.isCurrent = isCurrent
        self.isStale = isStale
        self.isHostFailed = isHostFailed


class DeployStatistics(object):
    def __init__(self, deploy=None, build=None, stageDistMap=None, stateDistMap=None, buildTag=None):
        self.deploy = deploy
        self.build = build
        self.buildTag = buildTag
        self.stageDistMap = stageDistMap
        self.stateDistMap = stateDistMap
        self.total = 0


class AgentReport(object):
    def __init__(self, firstTimeAgentStats=None, agentStats=None, currentDeployStat=None,
                 deprecatedDeployStats=None,
                 missingHosts=None, provisioningHosts=None, envName=None, stageName=None):
        self.firstTimeAgentStats = firstTimeAgentStats
        self.agentStats = agentStats
        self.currentDeployStat = currentDeployStat
        self.deprecatedDeployStats = deprecatedDeployStats
        self.missingHosts = missingHosts
        self.provisioningHosts = provisioningHosts
        self.envName = envName
        self.stageName = stageName
        self.showMode = 'complete'
        self.sortByStatus = 'false'

def genStageDistMap():
    stageDistMap = OrderedDict()
    for stage in environs_helper.DEPLOY_STAGE_VALUES:
        stageDistMap[stage] = 0
    return stageDistMap


def genStateDistMap():
    stateDistMap = {}
    for state in environs_helper.AGENT_STATE_VALUES:
        stateDistMap[state] = 0
    return stateDistMap


def addToEnvReport(request, deployStats, agent, env):
    deployId = agent['deployId']

    if deployId not in deployStats:
        deploy = deploys_helper.get(request, deployId)
        build = builds_helper.get_build(request, deploy['buildId'])
        stageDistMap = genStageDistMap()
        stateDistMap = genStateDistMap()
        deployStat = DeployStatistics(deploy=deploy, build=build, stageDistMap=stageDistMap,
                                      stateDistMap=stateDistMap)
        deployStats[deployId] = deployStat
    else:
        deployStat = deployStats[deployId]

    deployStat.stageDistMap[agent['deployStage']] += 1
    deployStat.stateDistMap[agent['state']] += 1
    deployStat.total += 1

    isCurrent = (deployId == env['deployId'])

    isStale = False
    duration = (time.time() * 1000 - agent['lastUpdateDate']) / 1000
    if duration >= DEFAULT_STALE_THRESHOLD:
        isStale = True

    isHostFailed = False
    agent_ec2_tags = agents_helper.get_agent_ec2_tags(request, env['envName'], env['stageName'])
    if agent_ec2_tags and agent_ec2_tags.get("service_mapping") == "shame":
        isHostFailed = True

    return AgentStatistics(agent, isCurrent, isStale, isHostFailed)


def _compare_agent_status(agentStats1, agentStats2):
    # Agents in failed states
    if agentStats1.agent['state'] < agentStats2.agent['state']:
        return 1
    elif agentStats1.agent['state'] > agentStats2.agent['state']:
        return -1

    if agentStats1.isCurrent and not agentStats2.isCurrent:
        return 1
    elif not agentStats1.isCurrent and agentStats2.isCurrent:
        return -1

    if agentStats1.agent['deployStage'] != agentStats2.agent['deployStage']:
        if agentStats1.agent['deployStage'] == "SERVING_BUILD":
            return 1
        else:
            return -1

    return 0


def gen_report(request, env, progress, sortByStatus="false"):
    agentStats = []
    firstTimeAgentStats = []
    deployStats = {}
    deprecatedDeployStats = []

    # always set the current
    deploy = deploys_helper.get(request, env['deployId'])
    build_info = builds_helper.get_build_and_tag(request, deploy["buildId"])
    stageDistMap = genStageDistMap()
    stateDistMap = genStateDistMap()
    currentDeployStat = DeployStatistics(deploy=deploy, build=build_info['build'], stageDistMap=stageDistMap,
                                         stateDistMap=stateDistMap, buildTag=build_info.get('tag'))
    deployStats[env['deployId']] = currentDeployStat

    # construct a map between host_id and account_id
    hosts = environ_hosts_helper.get_hosts(request, env['envName'], env['stageName'])
    accountIdMap = {}
    for host in hosts:
        accountIdMap[host['hostId']] = host['accountId']

    for agent in progress["agents"]:
        agent['accountId'] = accountIdMap.get(agent['hostId'])
        if agent["firstDeploy"]:
            firstTimeAgentStats.append(addToEnvReport(request, deployStats, agent, env))
        else:
            agentStats.append(addToEnvReport(request, deployStats, agent, env))

    if sortByStatus == "true":
        agentStats.sort(key=cmp_to_key(lambda x, y: _compare_agent_status(x, y)))

    for key, value in deployStats.items():
        if key != env['deployId']:
            deprecatedDeployStats.append(value)

    provisioning_hosts = progress["provisioningHosts"]

    return AgentReport(firstTimeAgentStats=firstTimeAgentStats,
                       agentStats=agentStats,
                       currentDeployStat=currentDeployStat,
                       deprecatedDeployStats=deprecatedDeployStats,
                       missingHosts=progress["missingHosts"],
                       provisioningHosts=provisioning_hosts,
                       envName=env['envName'], stageName=env['stageName'])


def gen_agent_by_deploy(progress, deployId, reportKind, deployStage=""):
    agent_wrapper = {}
    agent_wrapper[deployId] = []

    # get total alive hosts
    if deployStage == TOTAL_ALIVE_HOST_REPORT:
        for agent in progress['agents']:
            if agent['deployId'] == deployId:
                agent_wrapper[deployId].append(agent)

    # get unknown (unreachable) hosts
    elif reportKind == UNKNOWN_HOST_REPORT:
        for agent in progress['missingHosts']:
            # create a fake agent to pass into the agent wrapper
            missingAgent = {'hostName': agent, 'lastErrorCode': UNKNOWN_HOSTS_CODE}
            agent_wrapper[deployId].append(missingAgent)

    # get provisioning hosts
    elif reportKind == PROVISION_HOST_REPORT:
        for host in progress['provisioningHosts']:
            # create a fake agent to pass into the agent wrapper
            newHost = {'hostName': host.get('hostName'), 'hostId': host.get('hostId'), 'lastErrorCode': PROVISION_HOST_CODE}
            agent_wrapper[deployId].append(newHost)

    # get all hosts (alive + unknown)
    elif reportKind == TOTAL_HOST_REPORT:
        for agent in progress['agents']:
            agent_wrapper[deployId].append(agent)
        for agent in progress['missingHosts']:
            missingAgent = {'hostName': agent, 'lastErrorCode': UNKNOWN_HOSTS_CODE}
            agent_wrapper[deployId].append(missingAgent)

    # get all failed status
    elif reportKind == FAILED_HOST_REPORT:
        for agent in progress['agents']:
            if is_agent_failed(agent):
                agent_wrapper[deployId].append(agent)

    else:
        for agent in progress['agents']:
            if agent['deployId'] == deployId:
                if agent['deployStage'] == deployStage:
                    agent_wrapper[deployId].append(agent)

    return agent_wrapper
