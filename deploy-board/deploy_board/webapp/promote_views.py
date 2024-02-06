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
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.template.loader import render_to_string
from django.views.generic import View
from . import common
from .helpers import environs_helper
from .helpers.exceptions import TeletraanException, IllegalArgumentException


class EnvPromoteConfigView(View):
    def get(self, request, name, stage):
        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        env_promote = environs_helper.get_env_promotes_config(request, name, stage)
        stage_names = ['BUILD']
        for stage_name in stages:
            if stage_name != env['stageName']:
                stage_names.append(stage_name)

        if request.is_ajax():
            # return data for ajax calls
            html = render_to_string('configs/promote_config.tmpl', {
                "env": env,
                "stage_names": stage_names,
                "env_promote": env_promote,
                "csrf_token": get_token(request),
            })
            return HttpResponse(json.dumps({'html': html}), content_type="application/json")

        # otherwise, return a page
        return render(request, 'configs/promote_config.html', {
            "envs": envs,
            "env": env,
            "stages": stages,
            "stage_names": stage_names,
            "env_promote": env_promote,
        })

    def post(self, request, name, stage):
        query_dict = request.POST
        data = {}
        data["type"] = query_dict["promoteType"]
        data["predStage"] = query_dict["predStageName"]
        data["failPolicy"] = query_dict["promoteFailPolicy"]
        data["disablePolicy"] = query_dict["promoteDisablePolicy"]
        data["schedule"] = query_dict["promoteSchedule"]
        if 'promoteDelay' in query_dict:
            data["delay"] = int(query_dict["promoteDelay"])
        if "promoteQueueSize" in query_dict:
            data["queueSize"] = int(query_dict["promoteQueueSize"])
        try:
            environs_helper.update_env_promotes_config(request, name, stage, data=data)
        except TeletraanException as e:
            return HttpResponse(e, status=e.status, content_type="application/json")
        return self.get(request, name, stage)
