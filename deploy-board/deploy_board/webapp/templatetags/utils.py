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
"""Helper functions for template translate"""

import json
from django.conf import settings
from django import template
from datetime import datetime, timedelta
from collections import Mapping
import time
from math import trunc
import pytz
import logging
from deploy_board.webapp.service_add_ons import ServiceAddOn, LogHealthReport
from deploy_board.webapp.agent_report import (
    DEFAULT_STALE_THRESHOLD,
    UNKNOWN_HOSTS_CODE,
    PROVISION_HOST_CODE,
)
from deploy_board.webapp.common import is_agent_failed, BUILD_STAGE
from deploy_board.webapp.helpers import environs_helper
from deploy_board.webapp.helpers.tags_helper import TagValue
from deploy_board.settings import AWS_PRIMARY_ACCOUNT, AWS_SUB_ACCOUNT
import ast
from deploy_board.webapp.agent_report import AgentStatistics

register = template.Library()
logger = logging.getLogger(__name__)

_STATES_TO_TIPS = {
    "RUNNING": "Deploy is ongoing as expected",
    "FAILING": "Deploy is stuck",
    "SUCCEEDING": "Deploy is successful and still active",
    "SUCCEEDED": "Deploy was completed successfully",
    "ABORTED": "Deploy was replaced by a new one before successfully completed",
}

_HOTFIX_STATES_TO_TIPS = {
    "INITIAL": "Hotfix job is just created!",
    "PUSHING": "Cherry-picking commits and pushing to hotfix branch!",
    "BUILDING": "Building cherry-picked commits in hotfix branch!",
    "ABORTED": "Hotfix job is aborted!",
    "SUCCEEDED": "Hotfix job is completed successfully!",
    "FAILED": "Hotfix job is failed!",
}

_HOTFIX_STATES_TO_ICONS = {
    "INITIAL": "fa fa-spinner fa-spin",
    "PUSHING": "fa fa-spinner fa-spin",
    "BUILDING": "fa fa-spinner fa-spin",
    "ABORTED": "fa fa-minus-circle",
    "SUCCEEDED": "fa fa-check-circle color-green",
    "FAILED": "fa fa-circle color-red",
}

_TYPE_TO_TIPS = {
    "REGULAR": "REGULAR",
    "HOTFIX": "HOTFIX",
    "ROLLBACK": "ROLLBACK",
    "RESTART": "RESTART",
}

_TYPE_TO_ICONS = {
    "REGULAR": "glyphicon-plane",
    "HOTFIX": "glyphicon-fire",
    "ROLLBACK": "glyphicon-repeat icon-flipped",
    "RESTART": "glyphicon-refresh",
}

_ACCEPTANCE_TO_TIPS = {
    "PENDING_DEPLOY": "Deploy is ongoing, qualification is not started yet",
    "OUTSTANDING": "Depoly is waiting to be qualified",
    "PENDING_ACCEPT": "Deploy is being qualified",
    "ACCEPTED": "Deploy is qualified",
    "REJECTED": "Deploy failed to be qualified",
    "TERMINATED": "Deploy was aborted before qualification done",
}

_ACCEPTANCE_TO_ICONS = {
    "PENDING_DEPLOY": "glyphicon glyphicon-time",
    "OUTSTANDING": "glyphicon glyphicon-ok",
    "PENDING_ACCEPT": "glyphicon glyphicon-dashboard",
    "ACCEPTED": "glyphicon glyphicon-thumbs-up",
    "REJECTED": "glyphicon glyphicon-thumbs-down",
    "TERMINATED": "glyphicon glyphicon-warning-sign",
}

_STATE_TO_ICONS = {
    "RUNNING": "fa fa-spinner fa-spin",
    "FAILING": "fa fa-circle fa-blink color-red",
    "SUCCEEDING": "fa fa-circle color-green",
    "SUCCEEDED": "fa fa-check-circle color-green",
    "ABORTED": "fa fa-minus-circle",
}

_REPLACE_STATUS_TO_ICONS = {
    "Pending": "fa fa-spinner fa-spin",
    "Failed": "fa fa-circle color-red",
    "InProgress": "fa fa-refresh color-green",
    "Successful": "fa fa-check-circle color-green",
    "Cancelled": "fa fa-minus-circle",
    "Cancelling": "fa fa-refresh color-green",
    "RollbackInProgress": "fa fa-refresh color-green",
    "RollbackSuccessful": "fa fa-check-circle color-green",
    "RollbackFailed": "fa fa-circle color-red",
}

_REPLACE_STATUS_TO_TIPS = {
    "UNKNOWN": "Replacement is ongoing as expected",
    "FAILED": "Replacement is stuck",
    "SUCCEEDING": "Replacement is successful and still active",
    "SUCCEEDED": "Replacement was completed successfully",
    "ABORT": "Replacement was canceled",
    "TIMEOUT": "Replacement was timed-out due to no activity within a certain time (default 30 mins)",
}

_JENKINS_TO_ICONS = {
    "RUNNING": "fa fa-spinner fa-spin",
    "FAILURE": "fa fa-circle fa-blink color-red",
    "SUCCESS": "fa fa-check-circle color-green",
}

_STAGES_TO_TIPS = {
    "UNKNOWN": "Unexpected Stage",
    "PRE_DOWNLOAD": "Before download deploy payload",
    "DOWNLOADING": "Downloading deploy payload",
    "POST_DOWNLOAD": "After download deploy payload",
    "STAGING": "Staging deploy payload",
    "PRE_RESTART": "Before restart/start service",
    "RESTARTING": "Restarting/starting service",
    "POST_RESTART": "After restart/start service",
    "SERVING_BUILD": "Service is up and running",
    "STOPPING": "Stopping the service",
    "STOPPED": "Completed stopping",
}

_HEALTH_STATUS_TO_ICONS = {
    "UNKNOWN": "fa fa-spinner fa-spin",
    "SUCCEEDED": "fa fa-spinner fa-spin color-green",
    "QUALIFIED": "fa fa-check-circle color-green",
    "TELETRAAN_STOP_REQUESTED": "fa fa-check-circle color-green",
    "FAILED": "fa fa-circle fa-blink color-red",
    "TIMEOUT": "fa fa-minus-circle",
}

_HEALTH_STATE_TO_ICONS = {
    "INIT": "fa fa-spinner fa-spin",
    "LAUNCHING": "fa fa-spinner fa-spin",
    "PENDING_VERIFY": "fa fa-spinner fa-spin",
    "COMPLETING": "fa fa-spinner fa-spin",
    "COMPLETED": "fa fa-check-circle-o",
}

_HEALTH_TYPE_TO_ICONS = {
    "AMI_TRIGGERED": "fa fa-plus-circle",
    "TIME_TRIGGERED": "fa fa-clock-o",
    "MANUALLY_TRIGGERED": "fa fa-hand-o-left",
}

_TERMINATING_STATES = {
    "PENDING_TERMINATE",
    "TERMINATING",
    "PENDING_TERMINATE_NO_REPLACE",
}


# convert epoch (in milliseconds) to time
@register.filter("convertTimestamp")
def convertTimestamp(timestamp):
    if not timestamp:
        return "None"
    # return datetime.fromtimestamp(timestamp / 1000).strftime('%Y-%m-%d
    # %H:%M:%S')
    temp_time = datetime.fromtimestamp(
        float(timestamp) / 1000, pytz.timezone("America/Los_Angeles")
    )
    return temp_time.strftime("%Y-%m-%d %H:%M:%S")


@register.filter("computeDuration")
def computeDuration(timestamp):
    delta = timedelta(milliseconds=(time.time() * 1000 - timestamp))
    return str(delta - timedelta(microseconds=delta.microseconds))


@register.filter("computeElapsedTime")
def computeElapsedTime(deploy):
    delta = timedelta(milliseconds=(deploy["lastUpdateDate"] - deploy["startDate"]))
    return str(delta - timedelta(microseconds=delta.microseconds))


@register.filter("deployDurationTip")
def deployDurationTip(deploy):
    tip = "Deploy was started on %s(-08:00)." % convertTimestamp(deploy["startDate"])
    if deploy["successDate"]:
        tip = tip + " First succeeded on %s(-08:00)." % convertTimestamp(
            deploy["successDate"]
        )
    return tip


@register.filter("hotfixCanDeploy")
def hotfixCanDeploy(value):
    return value == "SUCCEEDED"


@register.filter("hotfixCanCancel")
def hotfixCanCancel(value):
    return value == "INITIAL" or value == "BUILDING" or value == "PUSHING"


@register.filter("isUnknownHost")
def isUnknownHost(value):
    return value == UNKNOWN_HOSTS_CODE


@register.filter("isProvisioningHost")
def isProvisioningHost(value):
    return value == PROVISION_HOST_CODE


@register.filter("agentRetryable")
def agentRetryable(agent):
    return (
        agent["state"] != "RESET"
        and agent["state"] != "PAUSED_BY_USER"
        and agent["state"] != "STOP"
    )


@register.filter("agentPausable")
def agentPausable(agent):
    return agent["state"] != "PAUSED_BY_USER"


@register.filter("agentPanelStatus")
def agentPanelStatus(agent):
    if agent["state"] == "PAUSED_BY_USER":
        return "panel-warning"
    return "panel-default"


@register.filter("shortenCommit")
def shortenCommit(value):
    return value[:7]


@register.filter("isHotfixBranch")
def isHotfixBranch(branch):
    return branch and branch.startswith("hotfix")


@register.assignment_tag(takes_context=True)
def get_deploy_priorities(context):
    return environs_helper.DEPLOY_PRIORITY_VALUES


@register.assignment_tag(takes_context=True)
def get_accept_types(context):
    return environs_helper.ACCEPTANCE_TYPE_VALUES


@register.assignment_tag(takes_context=True)
def get_promote_types(context):
    return environs_helper.PROMOTE_TYPE_VALUES


@register.assignment_tag(takes_context=True)
def get_web_hook_methods(context):
    return ["POST", "GET"]


@register.assignment_tag(takes_context=True)
def get_promote_fail_policies(context):
    return environs_helper.PROMOTE_FAILED_POLICY_VALUES


@register.assignment_tag(takes_context=True)
def get_promote_disable_policies(context):
    return environs_helper.PROMOTE_DISABLE_POLICY_VALUES


@register.assignment_tag(takes_context=True)
def get_override_policies(context):
    return environs_helper.OVERRIDE_POLICY_VALUES


@register.assignment_tag(takes_context=True)
def get_stage_types(context):
    return environs_helper.STAGE_TYPES


@register.assignment_tag(takes_context=True)
def get_deploy_constraint_types(context):
    return environs_helper.DEPLOY_CONSTRAINT_TYPES


@register.assignment_tag(takes_context=True)
def get_advanced_config_names(context):
    configNames = []
    for value in environs_helper.DEPLOY_STAGE_VALUES:
        if value != "UNKNOWN" and value != "SERVING_BUILD":
            configNames.append(value + ".process_timeout")
            configNames.append(value + ".termination_timeout")
            configNames.append(value + ".max_retry")
    configNames.append("target")
    return configNames


@register.filter("deployTypeIcon")
def deployTypeIcon(type):
    return _TYPE_TO_ICONS[type]


@register.filter("deployStateIcon")
def deployStateIcon(state):
    return _STATE_TO_ICONS[state]


@register.filter("replaceStatusIcon")
def replaceStatusIcon(state):
    return _REPLACE_STATUS_TO_ICONS[state]


@register.filter("hotfixStateIcon")
def hotfixStateIcon(state):
    return _HOTFIX_STATES_TO_ICONS[state]


@register.filter("jenkinsStateIcon")
def jenkinsStateIcon(state):
    return _JENKINS_TO_ICONS[state]


@register.filter("deployTypeTip")
def deployTypeTip(type):
    return _TYPE_TO_TIPS[type]


@register.filter("deployAcceptanceIcon")
def deployAcceptanceIcon(status):
    return _ACCEPTANCE_TO_ICONS[status]


@register.filter("deployAcceptanceTip")
def deployAcceptanceTip(status):
    return _ACCEPTANCE_TO_TIPS[status]


@register.filter("progressTip")
def progressTip(deploy):
    if deploy.get("account") == AWS_SUB_ACCOUNT:
        return "Among total %d hosts, %d are succeeded and %d are stuck" % (
            deploy["subAcctTotalHostNum"],
            deploy["subAcctSucHostNum"],
            deploy["subAcctFailHostNum"],
        )
    elif deploy.get("account") == AWS_PRIMARY_ACCOUNT:
        return "Among total %d hosts, %d are succeeded and %d are stuck" % (
            deploy["primaryAcctTotalHostNum"],
            deploy["primaryAcctSucHostNum"],
            deploy["primaryAcctFailHostNum"],
        )
    elif deploy.get("account") == "others":
        return "Among total %d hosts, %d are succeeded and %d are stuck" % (
            deploy["otherAcctTotalHostNum"],
            deploy["otherAcctSucHostNum"],
            deploy["otherAcctFailHostNum"],
        )
    else:
        return "Among total %d hosts, %d are succeeded and %d are stuck" % (
            deploy["total"],
            deploy["successTotal"],
            deploy["failTotal"],
        )


@register.filter("deployStateTip")
def deployStateTip(state):
    return _STATES_TO_TIPS[state]


@register.filter("replaceStatusTip")
def replaceStatusTip(state):
    return _REPLACE_STATUS_TO_TIPS[state]


@register.filter("hotfixStateTip")
def hotfixStateTip(state):
    return _HOTFIX_STATES_TO_TIPS[state]


@register.filter("isRollback")
def isRollback(deploy):
    return deploy["type"] == "ROLLBACK"


@register.filter("convertSuccThreshold")
def convertSuccThreshold(threshold):
    return threshold / float(100)


@register.filter("smartDate")
def smartDate(timestamp):
    """
    Get a datetime object or a int() Epoch timestamp and return a
    pretty string like 'an hour ago', 'Yesterday', '3 months ago',
    'just now', etc
    """
    now = datetime.now()
    diff = now - datetime.fromtimestamp(timestamp / 1000)
    second_diff = diff.seconds
    day_diff = diff.days

    if day_diff < 0:
        return ""

    if day_diff == 0:
        if second_diff < 10:
            return "just now"
        if second_diff < 60:
            return str(second_diff) + " seconds ago"
        if second_diff < 120:
            return "a minute ago"
        if second_diff < 3600:
            return str(second_diff // 60) + " minutes ago"
        if second_diff < 7200:
            return "an hour ago"
        if second_diff < 86400:
            return str(second_diff // 3600) + " hours ago"
    if day_diff == 1:
        return "Yesterday"
    if day_diff < 7:
        return str(day_diff) + " days ago"
    if day_diff < 31:
        return str(day_diff // 7) + " weeks ago"
    if day_diff < 365:
        return str(day_diff // 30) + " months ago"
    return str(day_diff // 365) + " years ago"


@register.filter("shortenDesc")
def shortenDesc(value):
    if not value or len(value) < 50:
        return value
    return value[:50] + "..."


def getTotalDuration(start, end=None):
    if end:
        delta = timedelta(milliseconds=(time.time() * 1000 - start))
    else:
        delta = timedelta(milliseconds=(end - start))
    return str(delta - timedelta(microseconds=delta.microseconds))


@register.filter("successRate")
def successRate(deploy):
    rate = 0
    if deploy.get("account") == AWS_SUB_ACCOUNT:
        if deploy["subAcctTotalHostNum"] != 0:
            rate = trunc(
                deploy["subAcctSucHostNum"] * 100 / deploy["subAcctTotalHostNum"]
            )
        return "%d%% (%d/%d)" % (
            rate,
            deploy["subAcctSucHostNum"],
            deploy["subAcctTotalHostNum"],
        )
    elif deploy.get("account") == AWS_PRIMARY_ACCOUNT:
        if deploy["primaryAcctTotalHostNum"] != 0:
            rate = trunc(
                deploy["primaryAcctSucHostNum"]
                * 100
                / deploy["primaryAcctTotalHostNum"]
            )
        return "%d%% (%d/%d)" % (
            rate,
            deploy["primaryAcctSucHostNum"],
            deploy["primaryAcctTotalHostNum"],
        )
    elif deploy.get("account") == "others":
        if deploy["otherAcctTotalHostNum"] != 0:
            rate = trunc(
                deploy["otherAcctSucHostNum"] * 100 / deploy["otherAcctTotalHostNum"]
            )
        return "%d%% (%d/%d)" % (
            rate,
            deploy["otherAcctSucHostNum"],
            deploy["otherAcctTotalHostNum"],
        )
    else:
        if deploy["total"] != 0:
            rate = trunc(deploy["successTotal"] * 100 / deploy["total"])
        return "%d%% (%d/%d)" % (rate, deploy["successTotal"], deploy["total"])


@register.filter("successRatePercentage")
def successRatePercentage(deploy):
    if deploy.get("account") == AWS_SUB_ACCOUNT:
        if deploy["subAcctTotalHostNum"] != 0:
            return trunc(
                deploy["subAcctSucHostNum"] * 100 / deploy["subAcctTotalHostNum"]
            )
    elif deploy.get("account") == AWS_PRIMARY_ACCOUNT:
        if deploy["primaryAcctTotalHostNum"] != 0:
            return trunc(
                deploy["primaryAcctSucHostNum"]
                * 100
                / deploy["primaryAcctTotalHostNum"]
            )
    elif deploy.get("account") == "others":
        if deploy["otherAcctTotalHostNum"] != 0:
            return trunc(
                deploy["otherAcctSucHostNum"] * 100 / deploy["otherAcctTotalHostNum"]
            )
    else:
        if deploy["total"] != 0:
            return trunc(deploy["successTotal"] * 100 / deploy["total"])
    return 0


@register.filter("successRateTip")
def successRateTip(deploy):
    return "Successfully installed on %d hosts out of total %d hosts." % (
        deploy["successTotal"],
        deploy["total"],
    )


@register.filter("hostStateClass")
def hostStateClass(state):
    if state in _TERMINATING_STATES:
        return "danger"
    else:
        return ""


@register.filter("agentStateClass")
def agentStateClass(state):
    if state == "UNREACHABLE":
        return "danger"
    else:
        return ""


@register.filter("warnIfOld")
def warnIfOld(timestamp):
    now = datetime.now()
    diff = now - datetime.fromtimestamp(timestamp / 1000)

    if diff.days >= settings.OLD_BUILD_WARNING_THRESHOLD_DAYS:
        return "WARNING: This build version is more than %d days old." % (
            settings.OLD_BUILD_WARNING_THRESHOLD_DAYS
        )
    else:
        return ""


# TODO is there a better way to handle this
@register.filter("commitRepoType")
def commitRepoType(repo):
    if "/" in repo:
        return "Github"
    else:
        return "Phabricator"


@register.filter("commitIcon")
def commitIcon(build):
    if "type" in build and build["type"]:
        type = build["type"].lower()
        if type == "phabricator":
            return "fa fa-eye"
        elif type == "github":
            return "fa fa-github"
    return ""


@register.filter("branchAndCommit")
def branchAndCommit(build):
    if build:
        return "%s/%s" % (build["branch"], build["commitShort"])
    else:
        return "UNKNOWN"


@register.filter("percentize")
def percentize(deploy):
    return "%d%%" % round(deploy.succeeded * 100 / deploy.reported)


@register.filter("agentTip")
def agentTip(agentStats):
    agent = agentStats.agent
    hostname = agent["hostName"]
    if agentStats.isStale:
        return "{}: Agent information is staled, click for more information".format(
            hostname
        )

    if agent["state"] == "PAUSED_BY_USER":
        return "{}: Agent is paused explicitly for any deploy".format(hostname)

    if agent["state"] == "PAUSED_BY_SYSTEM":
        return "{}: Agent is failed to deploy, click to see more details".format(
            hostname
        )

    if agent["state"] == "DELETE":
        return "{}: Agent is removed from current environment".format(hostname)

    if agent["state"] == "UNREACHABLE":
        return "{}: Agent is not reachable from teletraan server".format(hostname)

    if agent["state"] == "STOP":
        return "{}: Agent is gracefully shutting down the service".format(hostname)

    if agentStats.isCurrent:
        if agent["deployStage"] == "SERVING_BUILD":
            return "{}: Agent is serving the current build successfully".format(
                hostname
            )
        else:
            if is_agent_failed(agent):
                return "{}: Agent is deploying current build with failures, click to see more detail".format(
                    hostname
                )
            else:
                return "{}: Agent is deploying current build".format(hostname)
    else:
        if agent["deployStage"] == "SERVING_BUILD":
            return "{}: Agent is serving older build and waiting for deploy".format(
                hostname
            )
        else:
            if is_agent_failed(agent):
                return "{}: Agent is on older build with failures, click to see more detail".format(
                    hostname
                )
            else:
                return "{}: Agent is on older build and waiting for deploy".format(
                    hostname
                )


@register.filter("agentButton")
def agentButton(agentStats):
    agent = agentStats.agent

    if (
        agent["state"] == "PAUSED_BY_USER"
        or agent["state"] == "DELETE"
        or agent["state"] == "RESET"
    ):
        return "btn-info"

    if is_agent_failed(agent) or agent["state"] == "UNREACHABLE":
        return "btn-danger"

    if agent["state"] == "STOP":
        return "btn-warning"

    # normal state
    if agent["deployStage"] == "SERVING_BUILD" and agentStats.isCurrent:
        return "btn-default"

    if agentStats.isCurrent:
        return "btn-primary"

    return "btn-warning"


def _is_agent_failed(agent) -> bool:
    return (
        agent["status"] != "SUCCEEDED"
        and agent["status"] != "UNKNOWN"
        or agent["state"] == "PAUSED_BY_SYSTEM"
    )


@register.filter("hostButtonHtmlClass")
def hostButtonHtmlClass(agentStat: AgentStatistics) -> str:
    state = agentStat.agent["state"]
    if state == "UNREACHABLE":
        return "btn btn-default btn-xs host-stale btn-critical"
    elif _is_agent_failed(agentStat.agent):
        return "btn btn-default btn-xs btn-outline-danger"
    elif state == "DELETE" or state == "RESET":
        return "btn btn-default btn-xs btn-info"
    elif state == "STOP":
        return "btn btn-default btn-xs btn-outline-info"
    elif state == "PAUSED_BY_USER":
        return "btn btn-default btn-xs btn-outline-warning"
    return "btn btn-default btn-xs"


@register.filter("hostHealthcheckIcon")
def hostHealthcheckIcon(agentStat: AgentStatistics) -> str:
    state = agentStat.agent["state"]
    if state == "DELETE":
        return "fa fa-trash"
    elif state == "RESET":
        return "fa fa-repeat fa-spin"
    elif state == "UNREACHABLE":
        return "fa fa-question"
    elif state == "STOP":
        return "fa fa-stop"
    healthcheckResponse: str = agentStat.agent.get("containerHealthStatus", "")
    if "unhealthy" in healthcheckResponse:
        return "fa fa-circle color-red"
    elif "healthy" in healthcheckResponse:
        return "fa fa-circle color-green"
    return ""


@register.filter("hostTooltipTitle")
def hostTooltipTitle(agentStat: AgentStatistics) -> str:
    state = agentStat.agent["state"]
    if state == "PAUSED_BY_USER":
        return "Agent is explicitly paused for any deploys"
    elif state == "PAUSED_BY_SYSTEM":
        return "Agent failed to deploy and is paused by the system"
    elif state == "DELETE":
        return "Agent is removed from the current environment"
    elif state == "UNREACHABLE":
        return "Agent is unreachable after not pinging Teletraan control plane within the threshold"
    elif state == "STOP":
        return "Agent is gracefully shutting down the service"
    elif state == "RESET":
        return "Agent is restarting the deployment"
    elif not agentStat.isCurrent:
        if state == "SERVING_BUILD":
            return "Agent is serving older build and waiting for deploy"
        elif _is_agent_failed(agentStat.agent):
            return "Agent is serving older build with failures. Click for details"
        else:
            return "Agent is serving older build with failures. Click for details"
    else:
        if agentStat.agent["deployStage"] == "SERVING_BUILD":
            return "Agent is serving the current build successfully"
        elif _is_agent_failed(agentStat.agent):
            return "Agent is deploying current build with failures. Click for details"
        else:
            return "Agent is deploying current build"


@register.filter("agentIcon")
def agentIcon(agentStats):
    agent = agentStats.agent

    if agent["state"] == "PAUSED_BY_USER":
        return "fa-pause"

    if agent["state"] == "PAUSED_BY_SYSTEM":
        return "fa-exclamation-triangle"

    if agent["state"] == "DELETE":
        return "fa-trash"

    if agent["state"] == "RESET":
        return "fa-repeat"

    if agent["state"] == "UNREACHABLE":
        return "fa-question"

    if agent["state"] == "STOP":
        return "fa-recycle fa-spin"

    # normal state
    if agentStats.isCurrent:
        if agent["deployStage"] == "SERVING_BUILD":
            return "fa-check"
        if agentStats.isStale or (
            agent["state"] == "PROVISIONED" and agent["ip"] is None
        ):
            return "fa-exclamation-triangle"
        return "fa-spinner fa-spin"

    return "fa-clock-o"


@register.filter("hostButton")
def hostButton(host):
    if host["state"] in _TERMINATING_STATES:
        return "btn-warning"

    return "btn-default"


@register.filter("hostIcon")
def hostIcon(host):
    if host["state"] == "PROVISIONED":
        duration = (time.time() * 1000 - host["lastUpdateDate"]) / 1000
        isStale = duration > DEFAULT_STALE_THRESHOLD * 3
        if isStale and host["ip"] is None:
            return "fa-exclamation-triangle"
        return "fa-refresh fa-spin"

    if host["state"] == "ACTIVE":
        return "fa-check-square-o"

    return "fa-recycle fa-spin"


@register.filter("hostTip")
def hostTip(host):
    hostname = host["hostName"]
    if host["state"] == "PROVISIONED":
        return "{}: Host is provisioning, click for more information".format(hostname)

    if host["state"] == "ACTIVE":
        return "{}: Host is active and running, click for more information".format(
            hostname
        )

    return "{}: Host is marked for termination, click for more information".format(
        hostname
    )


@register.filter("jenkinsButton")
def jenkinsButton(current_status):
    if current_status == "FAILURE":
        return "btn-danger"

    if current_status == "SUCCESS":
        return "btn-default"

    if current_status == "RUNNING":
        return "btn-primary"

    return "btn-warning"


@register.filter("jenkinsIcon")
def jenkinsIcon(current_status):
    if current_status == "FAILURE":
        return "fa-exclamation-triangle"

    if current_status == "RUNNING":
        return "fa-spinner fa-spin"

    if current_status == "SUCCESS":
        return "fa-check"

    return "fa-clock-o"


@register.filter("isInstalling")
def isInstalling(agentStats):
    agent = agentStats.agent
    if (
        agent["state"] == "PAUSED_BY_USER"
        or agent["state"] == "PAUSED_BY_SYSTEM"
        or agent["state"] == "DELETE"
        or agent["state"] == "STOP"
    ):
        return False
    return agent["deployStage"] != "SERVING_BUILD"


@register.filter("canRollbackTo")
def canRollbackTo(deploy):
    if deploy["type"] == "RESTART" or deploy["type"] == "ROLLBACK":
        return False
    return deploy["state"] == "SUCCEEDED" or deploy["state"] == "ABORTED"


@register.filter("needRollbackWarn")
def needRollbackWarn(deploy):
    if deploy["acceptanceStatus"] != "ACCEPTED":
        return True
    return False


@register.filter("needConfigLoading")
def needConfigLoading(asg_status):
    if asg_status == "UNKNOWN":
        return True
    return False


@register.filter("hasScalingActivities")
def hasScalingActivities(asg_status):
    return asg_status != "UNKNOWN"


@register.filter("hasPredStage")
def hasPredStage(env_promote):
    if not env_promote["predStage"] or env_promote["predStage"] == BUILD_STAGE:
        return False
    return True


@register.filter("canResume")
def canResume(env):
    if env["envState"] == "PAUSED":
        return True
    return False


@register.filter("deployStageTip")
def deployStageTip(stage):
    return _STAGES_TO_TIPS[stage]


@register.filter("progressType")
def progressType(deploy):
    if deploy["state"] == "ABORTED" or deploy["state"] == "FAILING":
        return "progress-bar-danger"
    else:
        return "progress-bar-success"


@register.filter("jenkinsProgressType")
def jenkinsProgressType(current_status):
    if current_status == "FAILURE":
        return "progress-bar-danger"
    else:
        return "progress-bar-success"


@register.filter("lineNumber")
def lineNumber(value):
    if not value:
        return 1
    return value.count("\n") + 1


@register.filter("reportTotal")
def reportTotal(report):
    total = len(report.agentStats)
    if report.missingHosts or report.provisioningHosts:
        return total + len(report.missingHosts) + len(report.provisioningHosts)
    return total


@register.filter("atLeastOneAddOn")
def atLeastOneAddOn(addOns):
    if addOns is None:
        return False
    for addOn in addOns:
        if addOn.state != ServiceAddOn.UNKNOWN:
            return True
    return False


@register.filter("logHealthMetricTitle")
def logHealthMetricTitle(logHealthResult):
    if logHealthResult.state == LogHealthReport.ERROR:
        return ""

    # Rest of logic assumes valid lognames and topics lists

    title = " received by "

    # NOTE: Only Kafka logging supported so far.

    lognames = logHealthResult.lognames
    topics = logHealthResult.topics

    if len(lognames) == 1:
        if lognames[0] == "*":
            title = "Any logs" + title
        else:
            title = ('Log named "%s"' % lognames[0]) + title
    else:
        lognames = ['"' + log + '"' for log in lognames]
        title = "Logs " + ", ".join(lognames) + title

    if len(topics) == 1:
        if topics[0] == "*":
            title += "any Kafka topic"
        else:
            title += 'Kafka topic: "%s"' % topics[0]
    else:
        topics = ['"' + topic + '"' for topic in topics]
        title += "Kafka topics: " + ", ".join(topics)

    return title


@register.filter("logHealthMessage")
def logHealthMessage(logHealthResult):
    maxMinsAgoThreshold = logHealthResult.latestLogAgoMinsBeforeWarning
    if logHealthResult.state == LogHealthReport.STABLE:
        return " Last log received about: %s minute(s) ago" % (
            logHealthResult.lastLogMinutesAgo
        )
    elif logHealthResult.state == LogHealthReport.WARNING:
        return " No logs received in the last %s minute(s)" % (maxMinsAgoThreshold)
    elif logHealthResult.state == LogHealthReport.ERROR:
        return logHealthResult.errorMsg
    return ""


@register.filter("logHealthClass")
def logHealthClass(logHealthResult):
    if logHealthResult.state == LogHealthReport.STABLE:
        return "fa fa-circle color-green"
    elif logHealthResult.state == LogHealthReport.WARNING:
        return "fa fa-circle color-red"
    elif logHealthResult.state == LogHealthReport.ERROR:
        return "fa fa-times color-red"
    return ""


@register.filter("addOnButton")
def addOnButton(addOn):
    if addOn.state == ServiceAddOn.ON:
        return "btn-success"
    elif addOn.state == ServiceAddOn.UNKNOWN:
        return ""
    elif addOn.state == ServiceAddOn.PARTIAL:
        return "btn-warning"
    return "btn-default"


@register.filter("addOnIcon")
def addOnIcon(addOn):
    if addOn.state == ServiceAddOn.LOADING:
        return "fa fa-w fa-spinner fa-spin"
    else:
        return ""


@register.filter("stageToString")
def stageToString(value):
    if value == -1:
        return "rollback"
    elif value == 0:
        return "serving_build"
    elif value == 1:
        return "pre_deploy"
    elif value == 2:
        return "deploy_to_canary"
    elif value == 3:
        return "deploy_to_prod"
    else:
        return "post_deploy"


@register.filter("actionTypeTitle")
def actionTypeTitle(value):
    if value == "GROW":
        return "grow"
    else:
        return "shrink"


@register.filter("actionTypeHead")
def actionTypeHead(value):
    if value == "GROW":
        return "Alarm to scale up"
    else:
        return "Alarm to scale down"


@register.filter("itemToComparator")
def itemToComparator(value):
    if value == "GreaterThanOrEqualToThreshold":
        return ">="
    elif value == "GreaterThanThreshold":
        return ">"
    elif value == "LessThanOrEqualToThreshold":
        return "<="
    else:
        return "<"


@register.filter("genSubnetInfo")
def genSubnetInfo(value):
    return "{} | {} | {}".format(
        value.get("id"), value.get("info").get("tag"), value.get("info").get("zone")
    )


@register.filter("genSubnetIdZone")
def genSubnetIdZone(value):
    return "{} | {}".format(value.get("id"), value.get("info").get("zone"))


@register.filter("genSubnetId")
def genSubnetId(value):
    return "{}|{}|{}".format(
        value.get("id"), value.get("info").get("tag"), value.get("info").get("zone")
    )


@register.filter("genImageInfo")
def genImageInfo(value):
    temp_time = datetime.fromtimestamp(
        value.get("publish_date") / 1000, pytz.timezone("America/Los_Angeles")
    )
    symbol = "X"
    if value.get("qualified"):
        symbol = "V"
    return "{} | {} | {} | {}".format(
        value.get("abstract_name"),
        value.get("provider_name"),
        temp_time.strftime("%Y-%m-%d %H:%M:%S"),
        symbol,
    )


@register.filter("healthCheckStatusClass")
def healthCheckStatusClass(error_message):
    if error_message:
        return "danger"
    else:
        return "success"


@register.filter("healthCheckStatusIcon")
def healthStatusIcon(status):
    return _HEALTH_STATUS_TO_ICONS[status]


@register.filter("healthCheckStateIcon")
def healthStateIcon(state):
    return _HEALTH_STATE_TO_ICONS[state]


@register.filter("healthCheckTypeIcon")
def healthTypeIcon(type):
    return _HEALTH_TYPE_TO_ICONS[type]


@register.filter("computeElapsedTimeForHealthCheck")
def computeElapsedTimeForHealthCheck(check):
    delta = timedelta(milliseconds=(check["last_worked_on"] - check["start_time"]))
    return str(delta - timedelta(microseconds=delta.microseconds))


@register.filter("computeLaunchLatencyForHealthCheck")
def computeLaunchLatencyForHealthCheck(check):
    if check.get("host_launch_time") and check.get("deploy_complete_time"):
        delta = timedelta(
            milliseconds=(check["deploy_complete_time"] - check["host_launch_time"])
        )
        return str(delta - timedelta(microseconds=delta.microseconds))
    else:
        return str(0)


@register.filter("computeDeployLatencyForHealthCheck")
def computeDeployLatencyForHealthCheck(check):
    if check.get("deploy_start_time") and check.get("deploy_complete_time"):
        delta = timedelta(
            milliseconds=(check["deploy_complete_time"] - check["deploy_start_time"])
        )
        return str(delta - timedelta(microseconds=delta.microseconds))
    else:
        return str(0)


@register.filter("truncateWord")
def truncateWord(word):
    if len(word) > 7:
        return word[:7]
    else:
        return word


@register.filter("qualifiedIcon")
def isQualified(qualified):
    if qualified:
        return "fa fa-check-circle color-green"
    else:
        return ""


@register.filter("isEnvEnabled")
def isEnvEnabled(env):
    return env["state"] == "NORMAL"


@register.filter("isDisabledEnvTag")
def isDisabledEnvTag(env_tag):
    if env_tag and env_tag.get("value") == "DISABLE_ENV":
        return True
    return False


@register.filter("availableBuildTag")
def get_available_tag(tag):
    if tag is not None and tag["value"] == TagValue.BAD_BUILD:
        return "Good"
    else:
        return "Bad"


@register.filter("tagBuildId")
def get_tag_build_id(tag):
    if tag is not None:
        meta_info = tag.get("metaInfo", None)
        if meta_info is not None:
            build = json.loads(meta_info)
            return build["id"]
    return None


@register.filter("canReplaceCluster")
def canReplaceCluster(cluster):
    if cluster and cluster.get("state") and cluster.get("state") != "NORMAL":
        return False
    return True


@register.filter("getType")
def get_type(object):
    return type(object).__name__


@register.filter("getValue")
def get_value(dictionary, key):
    """return value from dict, OrderedDict, UserDict"""
    if not isinstance(dictionary, Mapping):
        return None
    return dictionary.get(key, None)


@register.filter("convertConfigHistoryString")
def convertConfigHistoryString(change):
    change = str(change)
    change = change.replace("false", "False")
    change = change.replace("true", "True")
    if change[:1] == "{" or change[:1] == "[":
        try:
            converted_string = ast.literal_eval(change)
            return converted_string
        except Exception:
            pass
    return change


@register.filter(name="lookup")
def getPhoboLink(mapping, key):
    return dict(mapping)[key]
