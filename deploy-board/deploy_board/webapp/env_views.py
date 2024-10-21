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
"""Collection of all env related views
"""
import functools
import re
from django.middleware.csrf import get_token
from django.shortcuts import render, redirect
from django.views.generic import View
from django.template.loader import render_to_string
from django.http import HttpResponse
from django.contrib import messages
from deploy_board.settings import IS_PINTEREST
from deploy_board.settings import TELETRAAN_DISABLE_CREATE_ENV_PAGE, TELETRAAN_REDIRECT_CREATE_ENV_PAGE_URL, \
    IS_DURING_CODE_FREEZE, TELETRAAN_CODE_FREEZE_URL, TELETRAAN_JIRA_SOURCE_URL, TELETRAAN_TRANSFER_OWNERSHIP_URL, TELETRAAN_RESOURCE_OWNERSHIP_WIKI_URL, HOST_TYPE_ROADMAP_LINK, STAGE_TYPE_INFO_LINK
from deploy_board.settings import DISPLAY_STOPPING_HOSTS
from deploy_board.settings import KAFKA_LOGGING_ADD_ON_ENVS
from deploy_board.settings import AWS_PRIMARY_ACCOUNT, AWS_SUB_ACCOUNT
from . import agent_report
from . import service_add_ons
from . import common
from .accounts import get_accounts, get_accounts_from_deploy, create_legacy_ui_account, add_account_from_cluster, add_legacy_accounts
import random
import json
from collections import Counter
from .helpers import builds_helper, environs_helper, agents_helper, ratings_helper, deploys_helper, \
    systems_helper, environ_hosts_helper, clusters_helper, tags_helper, baseimages_helper, schedules_helper, placements_helper, hosttypes_helper, \
    accounts_helper, autoscaling_groups_helper, hosts_helper
from .templatetags import utils
from .helpers.exceptions import TeletraanException
import math
from dateutil.parser import parse
import calendar
from deploy_board.webapp.agent_report import TOTAL_ALIVE_HOST_REPORT, TOTAL_HOST_REPORT, ALIVE_STAGE_HOST_REPORT, \
    FAILED_HOST_REPORT, UNKNOWN_HOST_REPORT, PROVISION_HOST_REPORT
from diff_match_patch import diff_match_patch
import traceback
import logging

ENV_COOKIE_NAME = 'teletraan.env.names'
ENV_COOKIE_CAPACITY = 5
DEFAULT_TOTAL_PAGES = 7
DEFAULT_PAGE_SIZE = 30
BUILD_STAGE = "BUILD"
DEFAULT_ROLLBACK_DEPLOY_NUM = 6

STATUS_COOKIE_NAME = 'sort-by-status'
MODE_COOKIE_NAME = 'show-mode'
ACCOUNT_COOKIE_NAME = 'account'

log = logging.getLogger(__name__)


class EnvListView(View):
    def get(self, request):
        index = int(request.GET.get('page_index', '1'))
        size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
        names = environs_helper.get_all_env_names(request, index=index, size=size)
        envs_tag = tags_helper.get_latest_by_target_id(request, 'TELETRAAN')
        return render(request, 'environs/envs_landing.html', {
            "names": names,
            "pageIndex": index,
            "pageSize": DEFAULT_PAGE_SIZE,
            "disablePrevious": index <= 1,
            "disableNext": len(names) < DEFAULT_PAGE_SIZE,
            "envs_tag": envs_tag,
            "disable_create_env_page": TELETRAAN_DISABLE_CREATE_ENV_PAGE,
            "redirect_create_env_page_url": TELETRAAN_REDIRECT_CREATE_ENV_PAGE_URL
        })


class OverrideItem(object):
    def __init__(self, key=None, root=None, override=None):
        self.key = key
        self.root = root
        self.override = override


def get_all_stages2(envs, stage):
    stages = []
    stageName = "stageName"
    env = None
    for temp in envs:
        if stage and stage == temp[stageName]:
            env = temp
        stages.append(temp[stageName])
    stages.sort()
    if not env:
        stage = stages[0]
        for temp in envs:
            if temp[stageName] == stage:
                env = temp
                break
    return stages, env


def _fetch_param_with_cookie(request, param_name, cookie_name, default):
    """Gets a parameter from the GET request, or from the cookie, or the default. """
    saved_value = request.COOKIES.get(cookie_name, default)
    return request.GET.get(param_name, saved_value)


def logging_status(request, name, stage):

    env = environs_helper.get_env_by_stage(request, name, stage)
    envs = environs_helper.get_all_env_stages(request, name)
    showMode = _fetch_param_with_cookie(
        request, 'showMode', MODE_COOKIE_NAME, 'complete')
    account = _fetch_param_with_cookie(
                request, 'account', ACCOUNT_COOKIE_NAME, 'all')
    sortByStatus = _fetch_param_with_cookie(
        request, 'sortByStatus', STATUS_COOKIE_NAME, 'true')

    html = render_to_string('deploys/deploy_logging_check_landing.tmpl', {
        "envs": envs,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "csrf_token": get_token(request),
        "panel_title": "Kafka logging for %s (%s)" % (name, stage),
        "env": env,
        "display_stopping_hosts": DISPLAY_STOPPING_HOSTS,
        "pinterest": IS_PINTEREST
    })

    response = HttpResponse(html)

    # save preferences
    response.set_cookie(MODE_COOKIE_NAME, showMode)
    response.set_cookie(ACCOUNT_COOKIE_NAME, account)
    response.set_cookie(STATUS_COOKIE_NAME, sortByStatus)

    return response


def check_logging_status(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)
    progress = deploys_helper.update_progress(request, name, stage)
    showMode = _fetch_param_with_cookie(
        request, 'showMode', MODE_COOKIE_NAME, 'complete')
    account = _fetch_param_with_cookie(
                request, 'account', ACCOUNT_COOKIE_NAME, 'all')
    sortByStatus = _fetch_param_with_cookie(
        request, 'sortByStatus', STATUS_COOKIE_NAME, 'true')

    lognames = request.GET.get('lognames', '')
    topics = request.GET.get('topics', '')

    configStr = "%s:%s" % (topics, lognames)

    report = agent_report.gen_report(request, env, progress, sortByStatus=sortByStatus)
    kafkaLoggingAddOn = service_add_ons.getKafkaLoggingAddOn(serviceName=name,
                                                             report=report,
                                                             configStr=configStr)
    report.showMode = showMode
    report.sortByStatus = sortByStatus
    html = render_to_string('deploys/deploy_logging_check_update.tmpl', {
        "logHealthResult": kafkaLoggingAddOn.logHealthReport
    })

    response = HttpResponse(html)

    # save preferences
    response.set_cookie(MODE_COOKIE_NAME, showMode)
    response.set_cookie(ACCOUNT_COOKIE_NAME, account)
    response.set_cookie(STATUS_COOKIE_NAME, sortByStatus)

    return response


def update_deploy_progress(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)
    progress = deploys_helper.update_progress(request, name, stage)
    showMode = _fetch_param_with_cookie(
        request, 'showMode', MODE_COOKIE_NAME, 'complete')
    account = _fetch_param_with_cookie(
                request, 'account', ACCOUNT_COOKIE_NAME, 'all')
    sortByStatus = _fetch_param_with_cookie(
        request, 'sortByStatus', STATUS_COOKIE_NAME, 'true')

    report = agent_report.gen_report(request, env, progress, sortByStatus=sortByStatus)

    report.showMode = showMode
    report.account = account
    report.sortByStatus = sortByStatus

    # Get the host number for subAcct and primaryAcct
    report.currentDeployStat.deploy["account"] = account
    report.currentDeployStat.deploy["subAcctTotalHostNum"] = 0
    report.currentDeployStat.deploy["subAcctSucHostNum"] = 0
    report.currentDeployStat.deploy["subAcctFailHostNum"] = 0
    report.currentDeployStat.deploy["primaryAcctTotalHostNum"] = 0
    report.currentDeployStat.deploy["primaryAcctSucHostNum"] = 0
    report.currentDeployStat.deploy["primaryAcctFailHostNum"] = 0
    report.currentDeployStat.deploy["otherAcctTotalHostNum"] = 0
    report.currentDeployStat.deploy["otherAcctSucHostNum"] = 0
    report.currentDeployStat.deploy["otherAcctFailHostNum"] = 0

    if account == AWS_SUB_ACCOUNT:
        for agentStat in report.agentStats:
            if agentStat.agent["accountId"] and agentStat.agent["accountId"] == AWS_SUB_ACCOUNT:
                report.currentDeployStat.deploy["subAcctTotalHostNum"] += 1
                if agentStat.agent["status"] == "SUCCEEDED":
                    report.currentDeployStat.deploy["subAcctSucHostNum"] += 1
                else:
                    report.currentDeployStat.deploy["subAcctFailHostNum"] += 1
    elif account == AWS_PRIMARY_ACCOUNT:
        for agentStat in report.agentStats:
            if not agentStat.agent["accountId"] or agentStat.agent["accountId"] == AWS_PRIMARY_ACCOUNT or agentStat.agent["accountId"] == "null":
                report.currentDeployStat.deploy["primaryAcctTotalHostNum"] += 1
                if agentStat.agent["status"] == "SUCCEEDED":
                    report.currentDeployStat.deploy["primaryAcctSucHostNum"] += 1
                else:
                    report.currentDeployStat.deploy["primaryAcctFailHostNum"] += 1
    elif account == "others":
        for agentStat in report.agentStats:
            if agentStat.agent["accountId"] and agentStat.agent["accountId"] != AWS_PRIMARY_ACCOUNT and agentStat.agent["accountId"] != AWS_SUB_ACCOUNT and agentStat.agent["accountId"] != "null":
                report.currentDeployStat.deploy["otherAcctTotalHostNum"] += 1
                if agentStat.agent["status"] == "SUCCEEDED":
                    report.currentDeployStat.deploy["otherAcctSucHostNum"] += 1
                else:
                    report.currentDeployStat.deploy["otherAcctFailHostNum"] += 1

    accounts = []
    cluster = clusters_helper.get_cluster(request, env.get('clusterName'))
    if cluster is not None:
        add_account_from_cluster(request, cluster, accounts)
    add_legacy_accounts(accounts, report)

    context = {
        "report": report,
        "env": env,
        "display_stopping_hosts": DISPLAY_STOPPING_HOSTS,
        "pinterest": IS_PINTEREST,
        "primaryAccount": AWS_PRIMARY_ACCOUNT,
        "subAccount": AWS_SUB_ACCOUNT,
        "accounts": accounts,
    }

    html = render_to_string('deploys/deploy_progress.html', context)

    response = HttpResponse(html)

    # save preferences
    response.set_cookie(MODE_COOKIE_NAME, showMode)
    response.set_cookie(ACCOUNT_COOKIE_NAME, account)
    response.set_cookie(STATUS_COOKIE_NAME, sortByStatus)

    return response


def update_service_add_ons(request, name, stage):
    serviceAddOns = []
    env = environs_helper.get_env_by_stage(request, name, stage)
    progress = deploys_helper.update_progress(request, name, stage)
    report = agent_report.gen_report(request, env, progress)
    metrics = environs_helper.get_env_metrics_config(request, name, stage)
    metricsDashboardUrl = None
    isSidecar = env['systemPriority'] is not None
    for metric in metrics:
        if metric['title'] == "dashboard":
            metricsDashboardUrl = metric['url']
    # Currently we assume that the servicename is the same as the environment name.
    serviceName = name
    rateLimitingAddOn = service_add_ons.getRatelimitingAddOn(serviceName=serviceName,
                                                             report=report)
    dashboardAddOn = service_add_ons.getDashboardAddOn(serviceName=serviceName,
                                                       metricsDashboardUrl=metricsDashboardUrl,
                                                       report=report, isSidecar=isSidecar)
    serviceAddOns.append(rateLimitingAddOn)

    if name in KAFKA_LOGGING_ADD_ON_ENVS:
        kafkaLoggingAddOn = service_add_ons.getKafkaLoggingAddOn(serviceName=serviceName,
                                                                 report=report)
        serviceAddOns.append(kafkaLoggingAddOn)

    serviceAddOns.append(dashboardAddOn)

    html = render_to_string('deploys/deploy_add_ons.tmpl', {
        "serviceAddOns": serviceAddOns,
        "pinterest": IS_PINTEREST
    })

    response = HttpResponse(html)
    return response


def removeEnvCookie(request, name):
    if ENV_COOKIE_NAME in request.COOKIES:
        cookie = request.COOKIES[ENV_COOKIE_NAME]
        saved_names = cookie.split(',')
        names = []
        total = 0
        for saved_name in saved_names:
            if total >= ENV_COOKIE_CAPACITY:
                break
            if not saved_name == name:
                names.append(saved_name)
                total += 1
        return ','.join(names)
    else:
        return ""


def genEnvCookie(request, name):
    if ENV_COOKIE_NAME in request.COOKIES:
        # keep 5 recent visited env
        cookie = request.COOKIES[ENV_COOKIE_NAME]
        saved_names = cookie.split(',')
        names = [name]
        total = 1
        for saved_name in saved_names:
            if total >= ENV_COOKIE_CAPACITY:
                break
            if not saved_name == name:
                names.append(saved_name)
                total += 1
        return ','.join(names)
    else:
        return name


def getRecentEnvNames(request):
    if ENV_COOKIE_NAME in request.COOKIES:
        cookie = request.COOKIES[ENV_COOKIE_NAME]
        return cookie.split(',')
    else:
        return None


def get_recent_envs(request):
    names = getRecentEnvNames(request)
    html = render_to_string('environs/simple_envs.tmpl', {
        "envNames": names,
        "isPinterest": IS_PINTEREST,
    })
    return HttpResponse(html)


def check_feedback_eligible(request, username):
    # Checks to see if a user should be asked for feedback or not.
    try:
        if username and ratings_helper.is_user_eligible(request, username) and IS_PINTEREST:
            num = random.randrange(0, 100)
            if num <= 10:
                return True
        return False
    except Exception:
        log.error(traceback.format_exc())
        return False


class EnvLandingView(View):
    def get(self, request, name, stage=None):
        envs = environs_helper.get_all_env_stages(request, name)

        if len(envs) == 0:
            return redirect('/')

        stages, env = common.get_all_stages(envs, stage)
        env_promote = environs_helper.get_env_promotes_config(request, name, env['stageName'])
        stage = env['stageName']
        username = request.teletraan_user_id.name
        request_feedback = check_feedback_eligible(request, username)
        groups = environs_helper.get_env_capacity(request, name, stage, capacity_type="GROUP")
        metrics = environs_helper.get_env_metrics_config(request, name, stage)

        metrics_dashboard_only = False
        for metric in metrics:
            if metric['title'] == "dashboard" and len(metrics) == 1:
                metrics_dashboard_only = True

        alarms = environs_helper.get_env_alarms_config(request, name, stage)
        env_tag = tags_helper.get_latest_by_target_id(request, env['id'])
        basic_cluster_info = None
        capacity_info = {'groups': groups}

        project_name_is_default = False
        stage_with_external_id = None
        existing_stage_identifier = None
        for env_stage in envs:
            if env_stage['externalId'] is not None and env_stage['stageName'] == stage:
                stage_with_external_id = env_stage
                break

        if env["stageType"] == "DEFAULT" and env["systemPriority"] is None:
            stageTypeWikiInfo = {}
            stageTypeWikiInfo['link'] = STAGE_TYPE_INFO_LINK
            stageTypeWikiInfo['text'] = "pinch/teletraan-stagetypes"
            messages.add_message(request, messages.ERROR, "Please update the Stage Type to a value other than DEFAULT. See more details at ", stageTypeWikiInfo)

        if stage_with_external_id is not None and stage_with_external_id['externalId'] is not None:
            try:
                existing_stage_identifier = environs_helper.get_nimbus_identifier(request, stage_with_external_id['externalId'])
                project_name_is_default = True if existing_stage_identifier is not None and existing_stage_identifier['projectName'] == "default" else False
            except TeletraanException as detail:
                log.error('Handling TeletraanException when trying to access nimbus API, error message {}'.format(detail))
                messages.add_message(request, messages.ERROR, detail)

        project_info = None
        if existing_stage_identifier:
            project_name = existing_stage_identifier.get('projectName', None)
            if project_name:
                project_info = {}
                project_info['project_name'] = project_name
            try:
                project_info['project_url'] = environs_helper.get_nimbus_project_console_url(project_name)
            except TeletraanException as detail:
                log.error('Handling TeletraanException when trying to access nimbus API, error message {}'.format(detail))
                messages.add_message(request, messages.ERROR, detail)
        accounts = []
        if IS_PINTEREST:
            pindeploy_config = environs_helper.get_env_pindeploy(request, env['envName'], env['stageName'])
            basic_cluster_info = clusters_helper.get_cluster(request, env.get('clusterName'))
            capacity_info['cluster'] = basic_cluster_info
            placements = None
            remaining_capacity = None
            if capacity_info['cluster']:
                add_account_from_cluster(request, basic_cluster_info, accounts)
                account_id = basic_cluster_info.get("accountId")
                placements = placements_helper.get_simplified_by_ids(
                        request, account_id, basic_cluster_info['placement'],
                    basic_cluster_info['provider'], basic_cluster_info['cellName'])
                remaining_capacity = functools.reduce(lambda s, e: s + e['capacity'], placements, 0)
                host_type = hosttypes_helper.get_by_id(request, basic_cluster_info['hostType'])
                host_type_blessed_status = host_type['blessed_status']
                if host_type_blessed_status == "DECOMMISSIONING" or host_type['retired'] is True:
                    messages.add_message(request, messages.ERROR, "This environment is currently using a cluster with an unblessed Instance Type. Please refer to " + HOST_TYPE_ROADMAP_LINK + " for the recommended Instance Type")
        last_cluster_refresh_status = _get_last_cluster_refresh_status(request, env)
        latest_succeeded_base_image_update_event = baseimages_helper.get_latest_succeeded_image_update_event_by_cluster(request, env.get('clusterName'))
        is_auto_refresh_enabled = _is_cluster_auto_refresh_enabled(request, env)

        cluster_refresh_suggestion_for_golden_ami = _gen_message_for_refreshing_cluster(request, last_cluster_refresh_status, latest_succeeded_base_image_update_event, env)

        asg_suspended_processes = _get_asg_suspended_processes(request, env) or []


        if not env['deployId']:
            capacity_hosts = deploys_helper.get_missing_hosts(request, name, stage)
            provisioning_hosts = deduplicate_hosts(environ_hosts_helper.get_hosts(request, name, stage))

            response = render(request, 'environs/env_landing.html', {
                "envs": envs,
                "env": env,
                "all_stage_types": sorted(environs_helper.STAGE_TYPES),
                "env_promote": env_promote,
                "stages": stages,
                "metrics": metrics,
                "metrics_dashboard_only": metrics_dashboard_only,
                "alarms": alarms,
                "request_feedback": request_feedback,
                "code_freeze": IS_DURING_CODE_FREEZE,
                "code_freeze_url": TELETRAAN_CODE_FREEZE_URL,
                "disable_create_env_page": TELETRAAN_DISABLE_CREATE_ENV_PAGE,
                "redirect_create_env_page_url": TELETRAAN_REDIRECT_CREATE_ENV_PAGE_URL,
                "jira_source_url": TELETRAAN_JIRA_SOURCE_URL,
                "transfer_ownership_url": TELETRAAN_TRANSFER_OWNERSHIP_URL,
                "resource_ownership_wiki_url": TELETRAAN_RESOURCE_OWNERSHIP_WIKI_URL,
                "groups": groups,
                "capacity_hosts": capacity_hosts,
                "provisioning_hosts": provisioning_hosts,
                "basic_cluster_info": basic_cluster_info,
                "capacity_info": json.dumps(capacity_info),
                "env_tag": env_tag,
                "pinterest": IS_PINTEREST,
                "csrf_token": get_token(request),
                "display_stopping_hosts": DISPLAY_STOPPING_HOSTS,
                "project_name_is_default": project_name_is_default,
                "project_info": project_info,
                "remaining_capacity": json.dumps(remaining_capacity),
                "lastClusterRefreshStatus": last_cluster_refresh_status,
                "is_auto_refresh_enabled": is_auto_refresh_enabled,
                "cluster_refresh_suggestion_for_golden_ami": cluster_refresh_suggestion_for_golden_ami,
                "hasGroups": bool(capacity_info.get("groups")),
                "hasCluster": bool(capacity_info.get("cluster")),
                "primaryAccount": AWS_PRIMARY_ACCOUNT,
                "subAccount": AWS_SUB_ACCOUNT,
                "accounts": accounts,
                "pindeploy_config": pindeploy_config,
                "asg_suspended_processes": asg_suspended_processes,
            })
            showMode = 'complete'
            account = 'all'
            sortByStatus = 'true'
        else:
            # Get deploy progress
            progress = deploys_helper.update_progress(request, name, stage)
            showMode = _fetch_param_with_cookie(
                request, 'showMode', MODE_COOKIE_NAME, 'complete')
            account = _fetch_param_with_cookie(
                request, 'account', ACCOUNT_COOKIE_NAME, 'all')
            sortByStatus = _fetch_param_with_cookie(
                request, 'sortByStatus', STATUS_COOKIE_NAME, 'true')
            report = agent_report.gen_report(request, env, progress, sortByStatus=sortByStatus)
            report.showMode = showMode
            report.account = account
            report.sortByStatus = sortByStatus

            # Get the host number for subAcct and primaryAcct
            report.currentDeployStat.deploy["account"] = account
            report.currentDeployStat.deploy["subAcctTotalHostNum"] = 0
            report.currentDeployStat.deploy["subAcctSucHostNum"] = 0
            report.currentDeployStat.deploy["subAcctFailHostNum"] = 0
            report.currentDeployStat.deploy["primaryAcctTotalHostNum"] = 0
            report.currentDeployStat.deploy["primaryAcctSucHostNum"] = 0
            report.currentDeployStat.deploy["primaryAcctFailHostNum"] = 0
            report.currentDeployStat.deploy["otherAcctTotalHostNum"] = 0
            report.currentDeployStat.deploy["otherAcctSucHostNum"] = 0
            report.currentDeployStat.deploy["otherAcctFailHostNum"] = 0

            if account == AWS_SUB_ACCOUNT:
                for agentStat in report.agentStats:
                    if agentStat.agent["accountId"] and agentStat.agent["accountId"] == AWS_SUB_ACCOUNT:
                        report.currentDeployStat.deploy["subAcctTotalHostNum"] += 1
                        if agentStat.agent["status"] == "SUCCEEDED":
                            report.currentDeployStat.deploy["subAcctSucHostNum"] += 1
                        else:
                            report.currentDeployStat.deploy["subAcctFailHostNum"] += 1
            elif account == AWS_PRIMARY_ACCOUNT:
                for agentStat in report.agentStats:
                    if not agentStat.agent["accountId"] or agentStat.agent["accountId"] == AWS_PRIMARY_ACCOUNT or agentStat.agent["accountId"] == "null":
                        report.currentDeployStat.deploy["primaryAcctTotalHostNum"] += 1
                        if agentStat.agent["status"] == "SUCCEEDED":
                            report.currentDeployStat.deploy["primaryAcctSucHostNum"] += 1
                        else:
                            report.currentDeployStat.deploy["primaryAcctFailHostNum"] += 1
            elif account == "others":
                for agentStat in report.agentStats:
                    if agentStat.agent["accountId"] and agentStat.agent["accountId"] != AWS_PRIMARY_ACCOUNT and agentStat.agent["accountId"] != AWS_SUB_ACCOUNT and agentStat.agent["accountId"] != "null":
                        report.currentDeployStat.deploy["otherAcctTotalHostNum"] += 1
                        if agentStat.agent["status"] == "SUCCEEDED":
                            report.currentDeployStat.deploy["otherAcctSucHostNum"] += 1
                        else:
                            report.currentDeployStat.deploy["otherAcctFailHostNum"] += 1

            add_legacy_accounts(accounts, report)

            context = {
                "envs": envs,
                "env": env,
                "all_stage_types": sorted(environs_helper.STAGE_TYPES),
                "env_promote": env_promote,
                "stages": stages,
                "report": report,
                "has_deploy": True,
                "metrics": metrics,
                "metrics_dashboard_only": metrics_dashboard_only,
                "alarms": alarms,
                "request_feedback": request_feedback,
                "code_freeze": IS_DURING_CODE_FREEZE,
                "code_freeze_url": TELETRAAN_CODE_FREEZE_URL,
                "jira_source_url": TELETRAAN_JIRA_SOURCE_URL,
                "transfer_ownership_url": TELETRAAN_TRANSFER_OWNERSHIP_URL,
                "resource_ownership_wiki_url": TELETRAAN_RESOURCE_OWNERSHIP_WIKI_URL,
                "groups": groups,
                "basic_cluster_info": basic_cluster_info,
                "capacity_info": json.dumps(capacity_info),
                "env_tag": env_tag,
                "pinterest": IS_PINTEREST,
                "display_stopping_hosts": DISPLAY_STOPPING_HOSTS,
                "project_name_is_default": project_name_is_default,
                "project_info": project_info,
                "remaining_capacity": json.dumps(remaining_capacity),
                "lastClusterRefreshStatus": last_cluster_refresh_status,
                "is_auto_refresh_enabled": is_auto_refresh_enabled,
                "cluster_refresh_suggestion_for_golden_ami": cluster_refresh_suggestion_for_golden_ami,
                "hasGroups": bool(capacity_info.get("groups")),
                "hasCluster": bool(capacity_info.get("cluster")),
                "primaryAccount": AWS_PRIMARY_ACCOUNT,
                "subAccount": AWS_SUB_ACCOUNT,
                "accounts": accounts,
                "pindeploy_config": pindeploy_config,
                "asg_suspended_processes": asg_suspended_processes,
            }
            response = render(request, 'environs/env_landing.html', context)

        # save preferences
        response.set_cookie(ENV_COOKIE_NAME, genEnvCookie(request, name))
        response.set_cookie(MODE_COOKIE_NAME, showMode)
        response.set_cookie(ACCOUNT_COOKIE_NAME, account)
        response.set_cookie(STATUS_COOKIE_NAME, sortByStatus)

        return response


def deduplicate_hosts(hosts):
    results = []
    seen = set()
    for h in hosts:
        host_id = h['hostId']
        if host_id not in seen:
            seen.add(host_id)
            results.append(h)
    return results

def _get_asg_suspended_processes(request, env):
    try:
        cluster_name = get_cluster_name(request, env.get('envName'), env.get('stageName'), env=env)
        return autoscaling_groups_helper.get_disabled_asg_actions(request, cluster_name)
    except Exception:
        return None

def _gen_message_for_refreshing_cluster(request, last_cluster_refresh_status, latest_succeeded_base_image_update_event, env):
    try:
        group_name = get_group_name(request, env.get('envName'), env.get('stageName'), env=env)
        cluster_config = clusters_helper.get_cluster(request, group_name)
        aws_owner_id = accounts_helper.get_aws_owner_id_for_cluster(request, cluster_config)
        host_ami_dist = hosts_helper.query_cmdb(
            query="tags.Autoscaling:{} AND state:running".format(group_name),
            fields="cloud.aws.imageId",
            account_id=aws_owner_id
        )

        counter = Counter([x['cloud.aws.imageId'] for x in host_ami_dist.json()])
        amis = list(counter.keys())

        current_AMI = baseimages_helper.get_by_id(request, cluster_config["baseImageId"])
        current_AMI = current_AMI["provider_name"]

        any_host_with_outdated_ami = len(amis) > 1 or (len(amis) == 1 and amis[0] != current_AMI)

        if any_host_with_outdated_ami and latest_succeeded_base_image_update_event is not None:
            if last_cluster_refresh_status is None or last_cluster_refresh_status["startTime"] is None or last_cluster_refresh_status["startTime"] <= latest_succeeded_base_image_update_event["finish_time"]:
                return "The cluster was updated with a new AMI at {} PST and should be replaced to ensure the AMI is applied to all existing hosts.".format(utils.convertTimestamp(latest_succeeded_base_image_update_event["finish_time"]))

        return None

    except Exception:
        # in case of any exception, return None instead of showing the error on landing page
        return None


def _get_last_cluster_refresh_status(request, env):
    try:
        cluster_name = get_cluster_name(request, env.get('envName'), env.get('stageName'), env=env)
        replace_summaries = clusters_helper.get_cluster_replacement_status(
            request, data={"clusterName": cluster_name})

        if len(replace_summaries["clusterRollingUpdateStatuses"]) == 0:
            return None

        return replace_summaries["clusterRollingUpdateStatuses"][0]
    except Exception:
        return None

def _is_cluster_auto_refresh_enabled(request, env):
    try:
        cluster_name = get_cluster_name(request, env.get('envName'), env.get('stageName'), env=env)
        cluster_info = clusters_helper.get_cluster(request, cluster_name)

        return cluster_info["autoRefresh"]
    except Exception:
        return None


def _compute_range(totalItems, thisPageIndex, totalItemsPerPage, totalPagesToShow):
    totalPages = int(math.ceil(float(totalItems) / totalItemsPerPage))
    if totalItems <= 0:
        return list(range(0)), 0, 0

    halfPagesToShow = totalPagesToShow // 2
    startPageIndex = thisPageIndex - halfPagesToShow
    if startPageIndex <= 0:
        startPageIndex = 1
    endPageIndex = startPageIndex + totalPagesToShow
    if endPageIndex > totalPages:
        endPageIndex = totalPages + 1

    prevPageIndex = thisPageIndex - 1
    nextPageIndex = thisPageIndex + 1
    if nextPageIndex > totalPages:
        nextPageIndex = 0
    return list(range(startPageIndex, endPageIndex)), prevPageIndex, nextPageIndex


def _convert_time(date_str, time_str):
    # We use pacific time by default
    if not time_str:
        time_str = "00:00:00"
    datestr = "%s %s -08:00" % (date_str, time_str)
    dt = parse(datestr)
    return calendar.timegm(dt.utctimetuple()) * 1000


def _convert_2_timestamp(date_str):
    # We use pacific time by default
    dt = parse(date_str)
    return calendar.timegm(dt.utctimetuple()) * 1000


def _get_commit_info(request, commit, repo=None, branch='master'):
    # We try teletraan for commit info first, if not found, try backend
    builds = builds_helper.get_builds(request, commit=commit)
    if builds:
        build = builds[0]
        return build['repo'], build['branch'], build['commitDate']

    if not repo:
        # Without repo, we can not call github api, return None
        log.error("Repo is expected when query based on commit which has no build")
        return None, None, None

    try:
        commit_info = builds_helper.get_commit(request, repo, commit)
        return repo, branch, commit_info['date']
    except Exception:
        log.error(traceback.format_exc())
        return None, None, None


def _gen_deploy_query_filter(request, from_date, from_time, to_date, to_time, size, reverse_date,
                             operator, commit, repo, branch):
    filter = {}
    filter_text = ""
    query_string = ""
    bad_commit = False

    if commit:
        filter_text += "commit=%s, " % commit
        query_string += "commit=%s&" % commit

        if repo:
            filter_text += "repo=%s, " % repo
            query_string += "repo=%s&" % repo

        if branch:
            filter_text += "branch=%s, " % branch
            query_string += "branch=%s&" % branch

        repo, branch, commit_date = _get_commit_info(request, commit, repo, branch)

        if repo and branch and commit_date:
            filter['repo'] = repo
            filter['branch'] = branch
            filter['commit'] = commit
            filter['commitDate'] = commit_date
        else:
            bad_commit = True

    if from_date:
        if from_time:
            filter_text += "from=%sT%s, " % (from_date, from_time)
            query_string += "from_date=%s&from_time=%s&" % (from_date, from_time)
        else:
            filter_text += "from=%s, " % from_date
            query_string += "from_time=%s&" % from_time
        after = _convert_time(from_date, from_time)
        filter['after'] = after

    if to_date:
        if to_time:
            filter_text += "to=%sT%s, " % (to_date, to_time)
            query_string += "to_date=%s&to_time=%s&" % (to_date, to_time)
        else:
            filter_text += "to=%s, " % to_date
            query_string += "to_time=%s&" % to_time
        before = _convert_time(to_date, to_time)
        filter['before'] = before

    if reverse_date and reverse_date.lower() == "true":
        filter_text += "earliest deploy first, "
        filter['oldestFirst'] = True
        query_string += "reverse_date=true&"

    if operator:
        filter_text += "operator=%s, " % operator
        filter['operator'] = operator
        query_string += "operator=%s&" % operator

    if size != DEFAULT_PAGE_SIZE:
        filter_text += "page_size=%s, " % size
        query_string += "page_size=%s&" % size

    if filter_text:
        filter_title = "Filter (%s)" % filter_text[:-2]
    else:
        filter_title = "Filter"

    if bad_commit:
        return None, filter_title, query_string
    else:
        return filter, filter_title, query_string


def _gen_deploy_summary(request, deploys, for_env=None):
    deploy_summaries = []
    accounts = {}
    for deploy in deploys:
        if for_env:
            env = for_env
        else:
            env = environs_helper.get(request, deploy['envId'])
        build_with_tag = builds_helper.get_build_and_tag(request, deploy['buildId'])
        account = None
        if env and env.get("clusterName") is not None:
            cluster = clusters_helper.get_cluster(request, env["clusterName"])
            if cluster: 
                provider, cell, id = cluster["provider"], cluster["cellName"], cluster.get("accountId", None)
                account_key = (provider, cell, id)
                if account_key in accounts:
                    account = accounts[account_key]
                else:
                    account = accounts_helper.get_by_cell_and_id(request, cell, id, provider)
                    if account is None:
                        account = accounts_helper.get_default_account(request, cell, provider)
                    accounts[account_key] = account
        deploy_accounts = []
        if account is None and env and deploy and build_with_tag:
            # terraform deploy, get information from deploy report
            progress = deploys_helper.update_progress(request, env["envName"], env["stageName"])
            report = agent_report.gen_report(request, env, progress, deploy=deploy, build_info=build_with_tag)
            deploy_accounts = [create_legacy_ui_account(account) for account in get_accounts(report)]
        elif account:
            deploy_accounts = [account]

        summary = {}
        summary['deploy'] = deploy
        summary['env'] = env
        summary['build'] = build_with_tag['build']
        summary['buildTag'] = build_with_tag['tag']
        summary['deploy_accounts'] = deploy_accounts
        deploy_summaries.append(summary)
    return deploy_summaries


def get_all_deploys(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    from_date = request.GET.get('from_date')
    from_time = request.GET.get('from_time')
    to_date = request.GET.get('to_date')
    to_time = request.GET.get('to_time')
    commit = request.GET.get('commit')
    repo = request.GET.get('repo')
    branch = request.GET.get('branch')
    reverse_date = request.GET.get('reverse_date')
    operator = request.GET.get('operator')

    if not branch:
        branch = 'master'

    filter, filter_title, query_string = \
        _gen_deploy_query_filter(request, from_date, from_time, to_date, to_time, size,
                                 reverse_date, operator, commit, repo, branch)

    if filter is None:
        # specified a bad commit
        return render(request, 'deploys/all_history.html', {
            "deploy_summaries": [],
            "filter_title": filter_title,
            "pageIndex": index,
            "pageSize": size,
            "from_date": from_date,
            "from_time": from_time,
            "to_date": to_date,
            "to_time": to_time,
            "commit": commit,
            "repo": repo,
            "branch": branch,
            "reverse_date": reverse_date,
            "operator": operator,
            'pageRange': list(range(0)),
            "prevPageIndex": 0,
            "nextPageIndex": 0,
            "query_string": query_string,
        })

    filter['pageIndex'] = index
    filter['pageSize'] = size
    result = deploys_helper.get_all(request, **filter)

    deploy_summaries = _gen_deploy_summary(request, result['deploys'])

    page_range, prevPageIndex, nextPageIndex = _compute_range(result['total'], index, size,
                                                              DEFAULT_TOTAL_PAGES)

    return render(request, 'deploys/all_history.html', {
        "deploy_summaries": deploy_summaries,
        "filter_title": filter_title,
        "pageIndex": index,
        "pageSize": size,
        "from_date": from_date,
        "from_time": from_time,
        "to_date": to_date,
        "to_time": to_time,
        "commit": commit,
        "repo": repo,
        "branch": branch,
        "reverse_date": reverse_date,
        "operator": operator,
        'pageRange': page_range,
        "prevPageIndex": prevPageIndex,
        "nextPageIndex": nextPageIndex,
        "query_string": query_string,
    })


def get_env_deploys(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)

    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    from_date = request.GET.get('from_date', None)
    from_time = request.GET.get('from_time', None)
    to_date = request.GET.get('to_date', None)
    to_time = request.GET.get('to_time', None)
    commit = request.GET.get('commit', None)
    repo = request.GET.get('repo', None)
    branch = request.GET.get('branch', None)
    reverse_date = request.GET.get('reverse_date', None)
    operator = request.GET.get('operator', None)

    filter, filter_title, query_string = \
        _gen_deploy_query_filter(request, from_date, from_time, to_date, to_time, size,
                                 reverse_date, operator, commit, repo, branch)

    result = deploys_helper.get_all(request, envId=[env['id']], pageIndex=1,
                                    pageSize=DEFAULT_ROLLBACK_DEPLOY_NUM)
    deploys = result.get("deploys")

    # remove the first deploy if exists
    current_build_id = None
    if deploys:
        current_deploy = deploys.pop(0)
        current_build_id = current_deploy['buildId']

    if filter is None:
        return render(request, 'environs/env_history.html', {
            "envs": envs,
            "env": env,
            "all_stage_types": sorted(environs_helper.STAGE_TYPES),
            "stages": stages,
            "deploy_summaries": [],
            "filter_title": filter_title,
            "pageIndex": index,
            "pageSize": size,
            "from_date": from_date,
            "from_time": from_time,
            "to_date": to_date,
            "to_time": to_time,
            "commit": commit,
            "repo": repo,
            "branch": branch,
            "reverse_date": reverse_date,
            "operator": operator,
            'pageRange': list(range(0)),
            "prevPageIndex": 0,
            "nextPageIndex": 0,
            "query_string": query_string,
            "current_build_id": current_build_id,
            "pinterest": IS_PINTEREST
        })

    filter['envId'] = [env['id']]
    filter['pageIndex'] = index
    filter['pageSize'] = size
    result = deploys_helper.get_all(request, **filter)
     
    deploy_summaries = _gen_deploy_summary(request, result['deploys'], for_env=env)

    page_range, prevPageIndex, nextPageIndex = _compute_range(result['total'], index, size,
                                                              DEFAULT_TOTAL_PAGES)

    return render(request, 'environs/env_history.html', {
        "envs": envs,
        "env": env,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "stages": stages,
        "deploy_summaries": deploy_summaries,
        "filter_title": filter_title,
        "pageIndex": index,
        "pageSize": size,
        "from_date": from_date,
        "from_time": from_time,
        "to_date": to_date,
        "to_time": to_time,
        "commit": commit,
        "repo": repo,
        "branch": branch,
        "reverse_date": reverse_date,
        "operator": operator,
        'pageRange': page_range,
        "prevPageIndex": prevPageIndex,
        "nextPageIndex": nextPageIndex,
        "query_string": query_string,
        "current_build_id": current_build_id,
        "pinterest": IS_PINTEREST
    })


def get_env_names(request):
    # TODO create a loop to get all names
    max_size = 10000
    names = environs_helper.get_all_env_names(request, index=1, size=max_size)
    return HttpResponse(json.dumps(names), content_type="application/json")


def search_envs(request, filter):
    max_size = 10000
    names = environs_helper.get_all_env_names(request, name_filter=filter, index=1, size=max_size)

    if not names:
        return redirect('/envs/')

    if len(names) == 1:
        return redirect('/env/%s/' % names[0])
    envs_tag = tags_helper.get_latest_by_target_id(request, 'TELETRAAN')
    return render(request, 'environs/envs_landing.html', {
        "names": names,
        "pageIndex": 1,
        "pageSize": DEFAULT_PAGE_SIZE,
        "disablePrevious": True,
        "disableNext": True,
        "envs_tag": envs_tag,
        "disable_create_env_page": TELETRAAN_DISABLE_CREATE_ENV_PAGE,
        "redirect_create_env_page_url": TELETRAAN_REDIRECT_CREATE_ENV_PAGE_URL
    })


def post_create_env(request):
    # TODO how to validate envName
    data = request.POST
    env_name = data["env_name"]
    stage_name = data["stage_name"]
    stage_type = data.get("stageType")
    clone_env_name = data.get("clone_env_name")
    clone_stage_name = data.get("clone_stage_name")
    description = data.get('description')
    external_id = None

    if clone_env_name and clone_stage_name:
        try:
            external_id = environs_helper.create_identifier_for_new_stage(request, env_name, stage_name)
            common.clone_from_stage_name(request, env_name, stage_name, clone_env_name,
                                        clone_stage_name, stage_type, description, external_id)
        except TeletraanException as detail:
            message = 'Failed to create identifier for {}/{}: {}'.format(env_name, stage_name, detail)
            log.error(message)
            messages.add_message(request, messages.ERROR, message)
            if external_id:
                try:
                    environs_helper.delete_nimbus_identifier(request, external_id)
                except Exception:
                    message = 'Also failed to delete Nimbus identifier {}. Please verify that identifier no longer exists, Error Message: {}'.format(external_id, detail)
                    log.error(message)
            raise detail
    else:
        data = {}
        data['envName'] = env_name
        data['stageName'] = stage_name
        data['description'] = description
        environs_helper.create_env(request, data)
    return redirect('/env/' + env_name + '/' + stage_name + '/config/')


class EnvNewDeployView(View):
    def get(self, request, name, stage):
        env = environs_helper.get_env_by_stage(request, name, stage)
        env_promote = environs_helper.get_env_promotes_config(request, name, stage)
        current_build = None
        if 'deployId' in env and env['deployId']:
            deploy = deploys_helper.get(request, env['deployId'])
            current_build = builds_helper.get_build(request, deploy['buildId'])

        return render(request, 'deploys/new_deploy.html', {
            "env": env,
            "env_promote": env_promote,
            "buildName": env['buildName'],
            "current_build": current_build,
            "pageIndex": 1,
            "pageSize": common.DEFAULT_BUILD_SIZE,
            "stage_type_info_link": STAGE_TYPE_INFO_LINK,
        })

    def post(self, request, name, stage):
        common.deploy(request, name, stage)
        return redirect('/env/%s/%s/deploy/' % (name, stage))


def post_add_stage(request, name):
    """handler for creating a new stage depending on configuration (IS_PINTEREST, from_stage i.e. clone stage). """
    # TODO how to validate stage name
    data = request.POST
    stage = data.get("stage")
    stage_type = data.get("stageType")
    from_stage = data.get("from_stage")
    description = data.get("description")

    all_envs_stages = environs_helper.get_all_env_stages(request, name)
    stages, _ = common.get_all_stages(all_envs_stages, None)
    if from_stage and from_stage not in stages:
        raise Exception("Can not clone from non-existing stage!")

    external_id = None
    if from_stage:
        try:
            external_id = environs_helper.create_identifier_for_new_stage(request, name, stage)
            common.clone_from_stage_name(request, name, stage, name, from_stage, stage_type, description, external_id)
        except TeletraanException as detail:
            message = 'Failed to create stage {}/{}: {}'.format(name, stage, detail)
            log.error(message)
            messages.add_message(request, messages.ERROR, message)
            if external_id:
                try:
                    environs_helper.delete_nimbus_identifier(request, external_id)
                except TeletraanException as detail:
                    message = 'Also failed to delete Nimbus identifier {}. Please verify that identifier no longer exists, Error Message: {}'.format(external_id, detail)
                    log.error(message)
                    messages.add_message(request, messages.ERROR, message)
    else:
        try:
            external_id = environs_helper.create_identifier_for_new_stage(request, name, stage)
            common.create_simple_stage(request, name, stage, stage_type, description, external_id)
        except TeletraanException as detail:
            message = 'Failed to create stage {}, Error Message: {}'.format(stage, detail)
            log.error(message)
            messages.add_message(request, messages.ERROR, message)
            if external_id:
                try:
                    environs_helper.delete_nimbus_identifier(request, external_id)
                except TeletraanException as detail:
                    message = 'Also Failed to delete Nimbus identifier {}, Please verify that identifier no longer exists, Error Message: {}'.format(external_id, detail)
                    log.error(message)
                    messages.add_message(request, messages.ERROR, message)

    return redirect('/env/' + name + '/' + stage + '/config/')


def remove_stage(request, name, stage):
    # TODO so we need to make sure the capacity is empty???
    envs = environs_helper.get_all_env_stages(request, name)
    current_env_stage_with_external_id = None
    for env_stage in envs:
        if env_stage['externalId'] is not None and env_stage['stageName'] == stage:
            current_env_stage_with_external_id = env_stage
            break

    if current_env_stage_with_external_id is not None and current_env_stage_with_external_id['externalId'] is not None:
        try:
            environs_helper.delete_nimbus_identifier(request, current_env_stage_with_external_id['externalId'])
        except TeletraanException as detail:
            message = 'Failed to delete Nimbus identifier {}. Please verify that identifier no longer exists, Error Message: {}'.format(current_env_stage_with_external_id['externalId'], detail)
            log.error(message)
            messages.add_message(request, messages.ERROR, message)

    environs_helper.delete_env(request, name, stage)
    envs = environs_helper.get_all_env_stages(request, name)
    response = redirect('/env/' + name)

    if len(envs) == 0:
        cookie_response = removeEnvCookie(request, name)
        if not cookie_response:
            response.delete_cookie(ENV_COOKIE_NAME)
        else:
            response.set_cookie(ENV_COOKIE_NAME, cookie_response)

    return response

def get_pipeline_url_from_build_info(build):
    if build['publishInfo'] and re.findall("https://[\w\d\-\.]*/job/[\w\d\-\.]*/[\d]*(/)?", build['publishInfo']):
        return re.sub("/[\d]*(/)?$", '', build['publishInfo'])
    if build['publishInfo'] and re.findall("https://[\w\d\-\.]*/job/[\w\d\-\.]*/?", build['publishInfo']):
        return build['publishInfo']
    return False

def get_builds(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)
    env_promote = environs_helper.get_env_promotes_config(request, name, stage)
    show_lock = False
    if env_promote['type'] == 'AUTO' and env_promote['predStage'] and \
            env_promote['predStage'] == environs_helper.BUILD_STAGE:
        show_lock = True

    if 'buildName' not in env and not env['buildName']:
        html = render_to_string('builds/simple_builds.tmpl', {
            "builds": [],
            "env": env,
            "show_lock": show_lock,
        })
        return HttpResponse(html)

    current_publish_date = 0
    build_deploy_pipeline_url = False

    if 'deployId' in env and env['deployId']:
        deploy = deploys_helper.get(request, env['deployId'])
        build = builds_helper.get_build(request, deploy['buildId'])
        current_publish_date = build['publishDate']
        build_deploy_pipeline_url = get_pipeline_url_from_build_info(build)

    if not build_deploy_pipeline_url:
        result = deploys_helper.get_all(request, envId=env['id'], pageIndex=1,
                                    pageSize=DEFAULT_ROLLBACK_DEPLOY_NUM)
        deploys = result.get("deploys")
        for deploy in deploys:
            if deploy['buildId'] :
                build = builds_helper.get_build(request, deploy['buildId'])
                build_deploy_pipeline_url = get_pipeline_url_from_build_info(build)
                if build_deploy_pipeline_url:
                    break

    # return only the new builds
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', common.DEFAULT_BUILD_SIZE))
    builds = builds_helper.get_builds_and_tags(request, name=env['buildName'], pageIndex=index,
                                      pageSize=size)
    new_builds = []
    for build in builds:
        if build['build']['publishDate'] > current_publish_date:
            new_builds.append(build)

    html = render_to_string('builds/simple_builds.tmpl', {
        "builds": new_builds,
        "build_deploy_pipeline_url" : build_deploy_pipeline_url,
        "current_publish_date": current_publish_date,
        "env": env,
        "show_lock": show_lock,
    })
    return HttpResponse(html)


def get_groups(request, name, stage):
    groups = common.get_env_groups(request, name, stage)
    html = render_to_string('groups/simple_groups.tmpl', {
        "groups": groups,
    })
    return HttpResponse(html)


def deploy_build(request, name, stage, build_id):
    env = environs_helper.get_env_by_stage(request, name, stage)
    current_build = None
    deploy_state = None
    scmType = ""
    if env.get('deployId'):
        current_deploy = deploys_helper.get(request, env['deployId'])
        current_build = builds_helper.get_build(request, current_deploy['buildId'])
        deploy_state = deploys_helper.get(request, env['deployId'])['state']
        scmType = current_build['type']
    build = builds_helper.get_build_and_tag(request, build_id)
    builds = [build]
    scm_url = systems_helper.get_scm_url(request, scmType)

    html = render_to_string('deploys/deploy_build.html', {
        "env": env,
        "builds": builds,
        "current_build": current_build,
        "scm_url": scm_url,
        "buildName": env.get('buildName'),
        "branch": env.get('branch'),
        "csrf_token": get_token(request),
        "deployState": deploy_state,
        "overridePolicy": env.get('overridePolicy'),
    })
    return HttpResponse(html)


def deploy_commit(request, name, stage, commit):
    env = environs_helper.get_env_by_stage(request, name, stage)
    builds = builds_helper.get_builds_and_tags(request, name=env.get('buildName', ''), commit=commit)
    current_build = None
    scmType = ""
    if env.get('deployId'):
        deploy = deploys_helper.get(request, env['deployId'])
        current_build = builds_helper.get_build(request, deploy['buildId'])
        scmType = current_build['type']
    scm_url = systems_helper.get_scm_url(request, scmType)

    html = render_to_string('deploys/deploy_build.html', {
        "env": env,
        "builds": builds,
        "current_build": current_build,
        "scm_url": scm_url,
        "buildName": env.get('buildName'),
        "branch": env.get('branch'),
        "csrf_token": get_token(request),
    })
    return HttpResponse(html)


def promote_to(request, name, stage, deploy_id):
    query_dict = request.POST
    toStages = query_dict['toStages']
    description = query_dict['description']
    toStage = None
    for toStage in toStages.split(','):
        deploys_helper.promote(request, name, toStage, deploy_id, description)

    return redirect('/env/%s/%s/deploy/' % (name, toStage))


def restart(request, name, stage):
    common.restart(request, name, stage)
    return redirect('/env/%s/%s/deploy/' % (name, stage))


def rollback_to(request, name, stage, deploy_id):
    common.rollback_to(request, name, stage, deploy_id)
    return redirect('/env/%s/%s/deploy/' % (name, stage))


def rollback(request, name, stage):
    query_dict = request.GET
    to_deploy_id = query_dict.get('to_deploy_id', None)
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    result = deploys_helper.get_all(request, envId=[env['id']], pageIndex=1,
                                    pageSize=DEFAULT_ROLLBACK_DEPLOY_NUM)
    deploys = result.get("deploys")

    # remove the first deploy if exists
    current_build_id = None
    if deploys:
        current_deploy = deploys.pop(0)
        current_build_id = current_deploy['buildId']

    # append the build info
    deploy_summaries = []
    branch = None
    commit = None
    build_id = None
    for deploy in deploys:
        build_info = builds_helper.get_build_and_tag(request, deploy['buildId'])
        build = build_info["build"]
        tag = build_info.get("tag", None)
        summary = {}
        summary['deploy'] = deploy
        summary['build'] = build
        summary['tag'] = tag
        if not to_deploy_id and deploy['state'] == 'SUCCEEDED':
            to_deploy_id = deploy['id']
        if to_deploy_id and to_deploy_id == deploy['id']:
            branch = build['branch']
            commit = build['commitShort']
            build_id = build['id']
        deploy_summaries.append(summary)

    html = render_to_string("environs/env_rollback.html", {
        "envs": envs,
        "stages": stages,
        "env": env,
        "deploy_summaries": deploy_summaries,
        "to_deploy_id": to_deploy_id,
        "branch": branch,
        "commit": commit,
        "build_id": build_id,
        "current_build_id": current_build_id,
        "csrf_token": get_token(request),
    })
    return HttpResponse(html)


def get_deploy(request, name, stage, deploy_id):
    deploy = deploys_helper.get(request, deploy_id)
    build_with_tag = builds_helper.get_build_and_tag(request, deploy['buildId'])
    env = environs_helper.get_env_by_stage(request, name, stage)
    deploy_accounts = get_accounts_from_deploy(request, env, deploy, build_with_tag)
    return render(request, 'environs/env_deploy_details.html', {
        "deploy": deploy,
        "csrf_token": get_token(request),
        "build": build_with_tag["build"],
        "env": env,
        "deploy_accounts": deploy_accounts
    })


def promote(request, name, stage, deploy_id):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)

    env_wrappers = []
    for temp_env in envs:
        env_wrapper = {}
        env_wrapper["env"] = temp_env
        env_wrapper["env_promote"] = environs_helper.get_env_promotes_config(request,
                                                                             temp_env['envName'],
                                                                             temp_env['stageName'])
        env_wrappers.append(env_wrapper)

    deploy = deploys_helper.get(request, deploy_id)
    build = builds_helper.get_build(request, deploy['buildId'])

    html = render_to_string("environs/env_promote.html", {
        "envs": envs,
        "stages": stages,
        "env": env,
        "env_wrappers": env_wrappers,
        "deploy": deploy,
        "build": build,
        "csrf_token": get_token(request),
    })
    return HttpResponse(html)


def pause(request, name, stage):
    deploys_helper.pause(request, name, stage)
    return redirect('/env/%s/%s/deploy/' % (name, stage))


def resume(request, name, stage):
    deploys_helper.resume(request, name, stage)
    return redirect('/env/%s/%s/deploy/' % (name, stage))


def enable_env_change(request, name, stage):
    params = request.POST
    description = params.get('description')
    environs_helper.enable_env_changes(request, name, stage, description)
    return redirect('/env/%s/%s/deploy/' % (name, stage))


def disable_env_change(request, name, stage):
    params = request.POST
    description = params.get('description')
    environs_helper.disable_env_changes(request, name, stage, description)
    return redirect('/env/%s/%s/deploy/' % (name, stage))


def enable_all_env_change(request):
    params = request.POST
    description = params.get('description')
    environs_helper.enable_all_env_changes(request, description)
    return redirect('/envs/')


def disable_all_env_change(request):
    params = request.POST
    description = params.get('description')
    environs_helper.disable_all_env_changes(request, description)
    return redirect('/envs/')


# get all reachable hosts
def get_hosts(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    agents = agents_helper.get_agents(request, env['envName'], env['stageName'])
    if agents:
        sorted(agents, key=lambda x: x['hostName'])
    title = "All hosts"

    agents_wrapper = {}
    show_protected_hosts = request.GET.get("show_protected_hosts") is not None
    host_ids = []
    for agent in agents:
        if agent['deployId'] not in agents_wrapper:
            agents_wrapper[agent['deployId']] = []
        agents_wrapper[agent['deployId']].append(agent)
        host_ids.append(agent['hostId'])

    protected_hosts = []
    if show_protected_hosts:
        protected_hosts = hosts_helper.get_hosts_is_protected(request, host_ids)

    return render(request, 'environs/env_hosts.html', {
        "envs": envs,
        "env": env,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "stage": stage,
        "stages": stages,
        "agents_wrapper": agents_wrapper,
        "title": title,
        "protected_hosts": protected_hosts,
        "show_protected_hosts": show_protected_hosts
    })


# get total alive hosts (hostStage == -1000)
# get alive hosts by using deploy_id and its stage (hostStage = 0 ~ 8)
def get_hosts_by_deploy(request, name, stage, deploy_id):
    hostStageString = request.GET.get('hostStage')
    if hostStageString is None:
        hostStage = TOTAL_ALIVE_HOST_REPORT
    else:
        hostStage = hostStageString
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    progress = deploys_helper.update_progress(request, name, stage)
    agents_wrapper = agent_report.gen_agent_by_deploy(progress, deploy_id,
                                                      ALIVE_STAGE_HOST_REPORT, hostStage)
    title = "All hosts with deploy " + deploy_id

    return render(request, 'environs/env_hosts.html', {
        "envs": envs,
        "env": env,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "stages": stages,
        "agents_wrapper": agents_wrapper,
        "title": title,
    })


# reset all failed hosts for this env, this deploy
def reset_failed_hosts(request, name, stage, deploy_id):
    agents_helper.reset_failed_agents(request, name, stage, deploy_id)
    return HttpResponse(json.dumps({'html': ''}), content_type="application/json")


# retry failed deploy stage for this env, this host
def reset_deploy(request, name, stage, host_id):
    agents_helper.retry_deploy(request, name, stage, host_id)
    return HttpResponse(json.dumps({'html': ''}), content_type="application/json")

# retry all deploys for this host
def reset_all_environments(request, name, stage, host_id):
    agents_helper.reset_all_environments(request, host_id)
    return redirect('/env/{}/{}/'.format(name, stage))


# pause deploy for this this env, this host
def pause_deploy(request, name, stage, host_id):
    agents_helper.pause_deploy(request, name, stage, host_id)
    return HttpResponse(json.dumps({'html': ''}), content_type="application/json")

# resume deploy stage for this env, this host


def resume_deploy(request, name, stage, host_id):
    agents_helper.resume_deploy(request, name, stage, host_id)
    return HttpResponse(json.dumps({'html': ''}), content_type="application/json")

# pause hosts for this env and stage


def pause_hosts(request, name, stage):
    post_params = request.POST
    host_ids = None
    if 'hostIds' in post_params:
        hosts_str = post_params['hostIds']
        host_ids = [x.strip() for x in hosts_str.split(',')]
    environs_helper.pause_hosts(request, name, stage, host_ids)
    return redirect('/env/{}/{}/'.format(name, stage))

# resume hosts for this env and stage


def resume_hosts(request, name, stage):
    post_params = request.POST
    host_ids = None
    if 'hostIds' in post_params:
        hosts_str = post_params['hostIds']
        host_ids = [x.strip() for x in hosts_str.split(',')]
    environs_helper.resume_hosts(request, name, stage, host_ids)
    return redirect('/env/{}/{}/'.format(name, stage))

# reset hosts for this env and stage


def reset_hosts(request, name, stage):
    post_params = request.POST
    host_ids = None
    if 'hostIds' in post_params:
        hosts_str = post_params['hostIds']
        host_ids = [x.strip() for x in hosts_str.split(',')]
    environs_helper.reset_hosts(request, name, stage, host_ids)
    return redirect('/env/{}/{}/hosts/'.format(name, stage))


# get total unknown(unreachable) hosts
def get_unknown_hosts(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    progress = deploys_helper.update_progress(request, name, stage)
    agents_wrapper = agent_report.gen_agent_by_deploy(progress, env['deployId'],
                                                      UNKNOWN_HOST_REPORT)
    title = "Unknow hosts"

    return render(request, 'environs/env_hosts.html', {
        "envs": envs,
        "env": env,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "stages": stages,
        "agents_wrapper": agents_wrapper,
        "title": title,
    })


# get provisioning hosts
def get_provisioning_hosts(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    progress = deploys_helper.update_progress(request, name, stage)
    agents_wrapper = agent_report.gen_agent_by_deploy(progress, env['deployId'],
                                                      PROVISION_HOST_REPORT)
    title = "Provisioning hosts"

    return render(request, 'environs/env_hosts.html', {
        "envs": envs,
        "env": env,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "stages": stages,
        "agents_wrapper": agents_wrapper,
        "title": title,
    })


# get total (unknown+alive) hosts
def get_all_hosts(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    progress = deploys_helper.update_progress(request, name, stage)
    agents_wrapper = agent_report.gen_agent_by_deploy(progress, env['deployId'],
                                                      TOTAL_HOST_REPORT)
    title = "All hosts"

    return render(request, 'environs/env_hosts.html', {
        "envs": envs,
        "env": env,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "stages": stages,
        "agents_wrapper": agents_wrapper,
        "title": title,
    })


# get failed (but alive) hosts (agent status > 0)
def get_failed_hosts(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    progress = deploys_helper.update_progress(request, name, stage)
    agents_wrapper = agent_report.gen_agent_by_deploy(progress, env['deployId'],
                                                      FAILED_HOST_REPORT)
    failed_hosts = [agent['hostId'] for agent in agents_wrapper[env['deployId']]]
    host_ids = ",".join(failed_hosts)
    title = "Failed Hosts"

    return render(request, 'environs/env_hosts.html', {
        "envs": envs,
        "env": env,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "stages": stages,
        "agents_wrapper": agents_wrapper,
        "title": title,
        "is_retryable": True,
        "host_ids": host_ids,
        "pinterest": IS_PINTEREST,
    })


# get all sub account hosts
def get_sub_account_hosts(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    agents = agents_helper.get_agents(request, env['envName'], env['stageName'])
    if agents:
        sorted(agents, key=lambda x: x['hostName'])
    title = "Sub Account Hosts"

    # construct a map between host_id and account_id
    hosts = environ_hosts_helper.get_hosts(request, env['envName'], env['stageName'])
    accountIdMap = {}
    for host in hosts:
        accountIdMap[host['hostId']] = host['accountId']

    agents_wrapper = {}
    for agent in agents:
        if not accountIdMap.get(agent['hostId']) or accountIdMap.get(agent['hostId']) == "null" or accountIdMap.get(agent['hostId']) == AWS_PRIMARY_ACCOUNT:
            continue
        if agent['deployId'] not in agents_wrapper:
            agents_wrapper[agent['deployId']] = []
        agents_wrapper[agent['deployId']].append(agent)

    return render(request, 'environs/env_hosts.html', {
        "envs": envs,
        "env": env,
        "all_stage_types": sorted(environs_helper.STAGE_TYPES),
        "stages": stages,
        "agents_wrapper": agents_wrapper,
        "title": title,
    })


def get_pred_deploys(request, name, stage):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    env = environs_helper.get_env_by_stage(request, name, stage)
    env_promote = environs_helper.get_env_promotes_config(request, name, stage)

    show_lock = False
    predStage = env_promote.get('predStage')
    if env_promote['type'] != "MANUAL" and predStage:
        show_lock = True

    current_startDate = 0
    deploys = []
    if predStage and predStage != "BUILD":
        pred_env = environs_helper.get_env_by_stage(request, name, predStage)
        # Pre_env can be gone or we no longer promote from it
        if pred_env is not None:
            result = deploys_helper.get_all(request, envId=[pred_env['id']], pageIndex=index,
                                            pageSize=size)
            deploys = result["deploys"]
            if env.get('deployId'):
                deploy = deploys_helper.get(request, env['deployId'])
                build = builds_helper.get_build(request, deploy['buildId'])
                current_startDate = build['publishDate']

    deploy_wrappers = []
    for deploy in deploys:
        build = builds_helper.get_build(request, deploy['buildId'])
        if build['publishDate'] > current_startDate:
            deploy_wrapper = {}
            deploy_wrapper['deploy'] = deploy
            deploy_wrapper['build'] = build
            deploy_wrappers.append(deploy_wrapper)

    html = render_to_string('deploys/simple_pred_deploys.tmpl', {
        "deploy_wrappers": deploy_wrappers,
        "envName": name,
        "stageName": predStage,
        "show_lock": show_lock,
        "current_startDate": current_startDate,
    })
    return HttpResponse(html)


def warn_for_deploy(request, name, stage, buildId):
    """ Returns a warning message if:
    1. The build has been tagged as build build
    2. a build doesn't have a successful deploy on the preceding stage.

    TODO: we would have call backend twice since the getAllDeploys call does not support filtering on multiple states;
    Also, getAllDeploys return all deploys with commits after the specific commit, it would be good if there is options
    to return the exact matched deploys.
    """
    build_info = builds_helper.get_build_and_tag(request, buildId)
    build = build_info["build"]
    tag = build_info.get("tag")

    if tag is not None and tag["value"] == tags_helper.TagValue.BAD_BUILD:
        html = render_to_string('warn_deploy_bad_build.tmpl', {
            'tag': tag,
        })
        return HttpResponse(html)

    env_promote = environs_helper.get_env_promotes_config(request, name, stage)
    pred_stage = env_promote.get('predStageName')
    if not pred_stage or pred_stage == BUILD_STAGE:
        return HttpResponse("")

    pred_env = environs_helper.get_env_by_stage(request, name, pred_stage)

    filter = {}
    filter['envId'] = [pred_env['id']]
    filter['commit'] = build['commit']
    filter['repo'] = build['repo']
    filter['oldestFirst'] = True
    filter['deployState'] = "SUCCEEDING"
    filter['pageIndex'] = 1
    filter['pageSize'] = 1
    result = deploys_helper.get_all(request, **filter)
    succeeding_deploys = result['deploys']

    if succeeding_deploys:
        return HttpResponse("")

    filter['deployState'] = "SUCCEEDED"
    result = deploys_helper.get_all(request, **filter)
    succeeded_deploys = result['deploys']

    if succeeded_deploys:
        return HttpResponse("")

    html = render_to_string('warn_no_success_deploy_in_pred.tmpl', {
        'envName': name,
        'predStageName': pred_stage,
    })

    return HttpResponse(html)


def get_env_config_history(request, name, stage):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    env = environs_helper.get_env_by_stage(request, name, stage)
    configs = environs_helper.get_config_history(request, name, stage, index, size)
    for config in configs:
        replaced_config = config["configChange"].replace(",", ", ").replace("#", "%23").replace("\"", "%22")\
            .replace("{", "%7B").replace("}", "%7D").replace("_", "%5F")
        config["replaced_config"] = replaced_config

    excludedTypes = list(filter(None, request.GET.get("exclude", '').replace("%20", " ").split(",")))

    return render(request, 'configs/config_history.html', {
        "envName": name,
        "stageName": stage,
        "envId": env['id'],
        "configs": configs,
        "pageIndex": index,
        "pageSize": DEFAULT_PAGE_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(configs) < DEFAULT_PAGE_SIZE,
        "excludedTypes": excludedTypes
    })


def _parse_config_comparison(query_dict):
    configs = {}
    for key, value in query_dict.items():
        if key.startswith('chkbox_'):
            id = key[len('chkbox_'):]
            split_data = value.split('_')
            config_change = split_data[1]
            configs[id] = config_change
    return configs


def get_config_comparison(request, name, stage):
    configs = _parse_config_comparison(request.POST)
    if len(configs) > 1:
        ids = list(configs.keys())
        change1 = configs[ids[0]]
        change2 = configs[ids[1]]
        return HttpResponse(json.dumps({'change1': change1, 'change2': change2}),
                            content_type="application/json")
    return HttpResponse("", content_type="application/json")


def show_config_comparison(request, name, stage):
    change1 = request.GET.get('change1')
    change2 = request.GET.get('change2')
    diff_res = GenerateDiff()
    result = diff_res.diff_main(change1, change2)
    diff_res.diff_cleanupSemantic(result)
    old_change = diff_res.old_content(result)
    new_change = diff_res.new_content(result)

    return render(request, 'configs/env_config_comparison_result.html', {
        "envName": name,
        "stageName": stage,
        "oldChange": old_change,
        "newChange": new_change,
    })


def get_deploy_schedule(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)
    envs = environs_helper.get_all_env_stages(request, name)
    schedule_id = env.get('scheduleId', None)
    if schedule_id is not None:
        schedule = schedules_helper.get_schedule(request, name, stage, schedule_id)
    else:
        schedule = None
    agent_number = agents_helper.get_agents_total_by_env(request, env["id"])
    return render(request, 'deploys/deploy_schedule.html', {
        "envs": envs,
        "env": env,
        "schedule": schedule,
        "agent_number": agent_number,
    })


class GenerateDiff(diff_match_patch):
    def old_content(self, diffs):
        html = []
        for (flag, data) in diffs:
            text = (data.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>")
                    .replace(",", ",<br>"))

            if flag == self.DIFF_DELETE:
                html.append("""<b style=\"background:#FFB5B5;
                    \">%s</b>""" % text)
            elif flag == self.DIFF_EQUAL:
                html.append("<span>%s</span>" % text)
        return "".join(html)

    def new_content(self, diffs):
        html = []
        for (flag, data) in diffs:
            text = (data.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>")
                    .replace(",", ",<br>"))
            if flag == self.DIFF_INSERT:
                html.append("""<b style=\"background:#97f697;
                    \">%s</b>""" % text)
            elif flag == self.DIFF_EQUAL:
                html.append("<span>%s</span>" % text)
        return "".join(html)


def get_new_commits(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)
    current_deploy = deploys_helper.get(request, env['deployId'])
    current_build = builds_helper.get_build(request, current_deploy['buildId'])
    startSha = current_build['commit']
    repo = current_build['repo']
    scm_url = systems_helper.get_scm_url(request, current_build['type'])
    diffUrl = "%s/%s/compare/%s...%s" % (scm_url, repo, startSha, startSha)
    last_deploy = common.get_last_completed_deploy(request, env)
    if not last_deploy:
        return render(request, 'deploys/deploy_commits.html', {
            "env": env,
            "title": "No previous deploy found!",
            "startSha": startSha,
            "endSha": startSha,
            "repo": repo,
            "diffUrl": diffUrl,
        })

    last_build = builds_helper.get_build(request, last_deploy['buildId'])
    endSha = last_build['commit']
    diffUrl = "%s/%s/compare/%s...%s" % (scm_url, repo, endSha, startSha)
    return render(request, 'deploys/deploy_commits.html', {
        "env": env,
        "startSha": startSha,
        "endSha": endSha,
        "repo": repo,
        "title": "Commits since last deploy",
        "diffUrl": diffUrl,
    })


def _get_endSha(end_build):
    endSha = end_build['commit']
    endShaBranch = end_build['branch']
    # handle the case for hotfix build, where we should use base_commit of the hotfix.
    # The base_commit is recorded in the suffix of branch name, in the format of 'hotfix_operator_1234567'
    if endShaBranch.startswith('hotfix_'):
        endShaBranchSplits = endShaBranch.split('_')
        if len(endShaBranchSplits) >= 3:
            endSha = endShaBranchSplits[len(endShaBranchSplits)-1]  # only get the last split

    return endSha


def compare_deploys(request, name, stage):
    start_deploy_id = request.GET.get('start_deploy', None)
    start_deploy = deploys_helper.get(request, start_deploy_id)
    start_build = builds_helper.get_build(request, start_deploy['buildId'])
    startSha = start_build['commit']
    repo = start_build['repo']
    scm = start_build['type']

    end_deploy_id = request.GET.get('end_deploy', None)
    if end_deploy_id:
        end_deploy = deploys_helper.get(request, end_deploy_id)
    else:
        env = environs_helper.get_env_by_stage(request, name, stage)
        end_deploy = common.get_previous_deploy(request, env, start_deploy)
        if not end_deploy:
            end_deploy = start_deploy
    end_build = builds_helper.get_build(request, end_deploy['buildId'])
    endSha = _get_endSha(end_build)

    commits, truncated, new_start_sha = common.get_commits_batch(request, scm, repo, startSha,
                                                                 endSha, keep_first=True)

    html = render_to_string('builds/commits.tmpl', {
        "commits": commits,
        "start_sha": new_start_sha,
        "end_sha": endSha,
        "repo": repo,
        "scm": scm,
        "truncated": truncated,
        "show_checkbox": False,
    })

    return HttpResponse(html)


def compare_deploys_2(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)
    configs = {}
    for key, value in request.GET.items():
        if key.startswith('chkbox_'):
            index = key[len('chkbox_'):]
            configs[index] = value
    indexes = list(configs.keys())
    start_build_id = configs[indexes[0]]
    end_build_id = configs[indexes[1]]
    if int(indexes[0]) > int(indexes[1]):
        start_build_id = configs[indexes[1]]
        end_build_id = configs[indexes[0]]

    start_build = builds_helper.get_build(request, start_build_id)
    startSha = start_build['commit']
    repo = start_build['repo']
    end_build = builds_helper.get_build(request, end_build_id)
    endSha = _get_endSha(end_build)

    scm_url = systems_helper.get_scm_url(request, start_build['type'])
    diffUrl = "%s/%s/compare/%s...%s" % (scm_url, repo, endSha, startSha)
    return render(request, 'deploys/deploy_commits.html', {
        "env": env,
        "startSha": startSha,
        "endSha": endSha,
        "repo": repo,
        "title": "Commits between the two deploys",
        "diffUrl": diffUrl,
    })


def get_tag_message(request):
    envs_tag = tags_helper.get_latest_by_target_id(request, 'TELETRAAN')
    html = render_to_string('environs/tag_message.tmpl', {
        'envs_tag': envs_tag,
    })
    return HttpResponse(html)


def update_schedule(request, name, stage):
    post_params = request.POST
    data = {}
    data['cooldownTimes'] = post_params['cooldownTimes']
    data['hostNumbers'] = post_params['hostNumbers']
    data['totalSessions'] = post_params['totalSessions']
    schedules_helper.update_schedule(request, name, stage, data)
    return HttpResponse(json.dumps(''))


def delete_schedule(request, name, stage):
    schedules_helper.delete_schedule(request, name, stage)
    return HttpResponse(json.dumps(''))


def override_session(request, name, stage):
    session_num = request.GET.get('session_num')
    schedules_helper.override_session(request, name, stage, session_num)
    return HttpResponse(json.dumps(''))

def get_cluster_name(request, name, stage, env=None):
    cluster_name = '{}-{}'.format(name, stage)
    current_cluster = clusters_helper.get_cluster(request, cluster_name)
    if current_cluster is None:
        if env is None:
            env = environs_helper.get_env_by_stage(request, name, stage)
        cluster_name = env.get("clusterName")
    return cluster_name

def get_group_name(request, name, stage, env=None):
    group_name = '{}-{}'.format(name, stage)
    current_group = autoscaling_groups_helper.get_group_info(request, group_name)
    if current_group is None:
        if env is None:
            env = environs_helper.get_env_by_stage(request, name, stage)
        group_name = env.get("groupName")
    return group_name
