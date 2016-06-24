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
"""Collection of all deploy related views
"""
from deploy_board.settings import SITE_METRICS_CONFIGS
from django.middleware.csrf import get_token
import json
from django.shortcuts import render
from django.views.generic import View
from django.template.loader import render_to_string
from django.http import HttpResponse
from helpers import builds_helper, deploys_helper, environs_helper, tags_helper


DEFAULT_PAGE_SIZE = 30
DEFAULT_ONGOING_DEPLOY_SIZE = 10


def _get_ongoing_deploys(request, index, size):
    # ongoing deploys are defined as deploys with states as:
    deploy_states = ["RUNNING", "FAILING"]
    deployResult = deploys_helper.get_all(request, deployState=deploy_states,
                                          pageIndex=index, pageSize=size)
    deploy_summaries = []
    for deploy in deployResult['deploys']:
        env = environs_helper.get(request, deploy['envId'])
        build = builds_helper.get_build(request, deploy['buildId'])
        summary = {}
        summary['deploy'] = deploy
        summary['env'] = env
        summary['build'] = build
        deploy_summaries.append(summary)

    return deploy_summaries


def get_landing_page(request):
    envs_tag = tags_helper.get_latest_by_targe_id(request, 'TELETRAAN')
    metrics = SITE_METRICS_CONFIGS
    return render(request, 'landing.html', {
        "metrics": metrics,
        'envs_tag': envs_tag,
    })


def get_ongoing_deploys(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_ONGOING_DEPLOY_SIZE))
    deploy_summeries = _get_ongoing_deploys(request, index, size)
    html = render_to_string('deploys/ongoing_deploys.tmpl', {
        "deploy_summaries": deploy_summeries,
        "pageIndex": index,
        "pageSize": size,
        "disablePrevious": index <= 1,
        "disableNext": len(deploy_summeries) < DEFAULT_ONGOING_DEPLOY_SIZE,
    })
    return HttpResponse(html)

def get_daily_deploy_count(request):
    daily_deploy_count = deploys_helper.get_daily_deploy_count(request);
    html = render_to_string('deploys/daily_deploy_count.tmpl', {
        "daily_deploy_count": daily_deploy_count,
    })
    return HttpResponse(html)


def get_duplicate_commit_deploy_message(request, name, stage, buildId):
    current_deploy = deploys_helper.get_current(request, name, stage)
    current_build = builds_helper.get_build(request, current_deploy['buildId'])
    current_commit = current_build['commit']

    next_build = builds_helper.get_build(request, buildId)
    next_commit = next_build['commit']

    if current_commit == next_commit:
        return render(request, 'deploys/duplicate_commit_deploy_message.tmpl',{
                      "commit":next_build['commitShort']})
    return HttpResponse('')


class DeployView(View):
    def get(self, request, deploy_id):
        deploy = deploys_helper.get(request, deploy_id)
        build = builds_helper.get_build(request, deploy['buildId'])
        env = None
        if deploy.get('envId'):
            env = environs_helper.get(request, deploy['envId'])
        return render(request, 'deploys/deploy_details.html', {
            "deploy": deploy,
            "build": build,
            "csrf_token": get_token(request),
            "env": env,
        })


def inline_update(request):
    query_dict = request.POST
    name = query_dict["name"]
    value = query_dict["value"]
    deploy_id = query_dict["deploy_id"]
    if name == "description":
        deploys_helper.update(request, deploy_id, {"description": value})
    else:
        log.error("Unsupport deploy update on field " + name)
    return HttpResponse(json.dumps({'html': ''}), content_type="application/json")
