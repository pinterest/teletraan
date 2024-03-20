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
"""Collection of all env promote config views
"""
import json
import logging
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.views.generic import View
from .common import get_env_groups, get_all_stages
from .helpers import environs_helper, clusters_helper, autoscaling_groups_helper, placements_helper
from .helpers import baseimages_helper
from deploy_board.settings import IS_PINTEREST


logger = logging.getLogger(__name__)


class EnvCapacityConfigView(View):

    def get(self, request, name, stage):
        # cluster manager
        provider_list = None
        basic_cluster_info = None
        create_new = False
        adv = False
        env = environs_helper.get_env_by_stage(request, name, stage)
        cluster_name = env.get('clusterName')
        termination_limit = env.get('terminationLimit')
        placements = None
        if IS_PINTEREST:
            provider_list = baseimages_helper.get_all_providers(request)
            basic_cluster_info = clusters_helper.get_cluster(request, cluster_name)
            if basic_cluster_info:
                base_image_id = basic_cluster_info.get('baseImageId')
                base_image = baseimages_helper.get_by_id(request, base_image_id)
                asg_cluster = autoscaling_groups_helper.get_group_info(request, cluster_name)
                basic_cluster_info['asg_info'] = asg_cluster
                basic_cluster_info['base_image_info'] = base_image
                try:
                    account_id = basic_cluster_info.get('accountId')
                    placements = placements_helper.get_simplified_by_ids(
                        request, account_id, basic_cluster_info['placement'],
                        basic_cluster_info['provider'], basic_cluster_info['cellName'])
                except Exception as e:
                    logger.warning('Failed to get placements: {}'.format(e))

            params = request.GET
            if params.get('adv'):
                adv = params.get('adv')
            if params.get('create_new'):
                create_new = params.get('create_new')

        if request.is_ajax():
            # return data for ajax calls
            hosts = environs_helper.get_env_capacity(request, name, stage, capacity_type="HOST")
            groups = get_env_groups(request, name, stage)
            if cluster_name in groups:
                groups.remove(cluster_name)
            info = {
                "env": env,
                "hosts": hosts,
                "groups": groups,
                "csrf_token": get_token(request),
                'is_pinterest': IS_PINTEREST,
                'provider_list': provider_list,
                'basic_cluster_info': basic_cluster_info,
                'adv': adv,
                'create_new': create_new,
                'placements': placements,
            }
            return HttpResponse(json.dumps(info), content_type="application/json")

        # otherwise, return a page
        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = get_all_stages(envs, stage)
        hosts = environs_helper.get_env_capacity(request, name, stage, capacity_type="HOST")
        groups = get_env_groups(request, name, stage)
        if cluster_name in groups:
            groups.remove(cluster_name)
        data = {
            "envs": envs,
            "env": env,
            "stages": stages,
            "hosts": hosts,
            "groups": groups,
            'is_pinterest': IS_PINTEREST,
            'provider_list': provider_list,
            'basic_cluster_info': basic_cluster_info,
            'adv': adv,
            'create_new': create_new,
            'placements': placements,
            'termination_limit': termination_limit,
            'cluster_name': cluster_name,
        }
        data['info'] = json.dumps(data)
        return render(request, 'configs/capacity.html', data)

    def post(self, request, name, stage):
        logger.info("Post to capacity with data {0}".format(request.body))
        data = json.loads(request.body)
        hosts = data.get('hosts')
        if hosts is not None:
            environs_helper.update_env_capacity(request, name, stage, capacity_type="HOST", data=hosts)

        groups = data.get("groups")
        if groups is not None:
            environs_helper.update_env_capacity(request, name, stage, capacity_type="GROUP", data=groups)

        return self.get(request, name, stage)
