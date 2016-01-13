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
from deploy_board.settings import IS_PINTEREST
from django.shortcuts import render
from django.views.generic import View
from django.template.loader import render_to_string
from django.http import HttpResponse
from helpers import builds_helper, deploys_helper, environs_helper


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
    return render(request, 'landing.html', {
        "pinterest": IS_PINTEREST,
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
            "env": env,
        })
