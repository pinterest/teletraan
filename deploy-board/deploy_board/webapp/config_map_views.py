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
"""Collection of all map based config views
"""
import json
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.template.loader import render_to_string
from django.views.generic import View
from .common import get_all_stages
from .helpers import environs_helper

# advanced/agent config or agent config map
AC_FLAVOR = 'AC'
# script config map
SC_FLAVOR = 'SC'
CUSTOMIZATION = {}
AC = {}
AC['panelTitle'] = "Agent Config"
AC['flavor'] = AC_FLAVOR
AC['configMapPath'] = "agent"
CUSTOMIZATION[AC_FLAVOR] = AC

SC = {}
SC['panelTitle'] = "Script Config"
SC['flavor'] = SC_FLAVOR
SC['configMapPath'] = "script"
CUSTOMIZATION[SC_FLAVOR] = SC


class EnvConfigMapView(View):
    def get(self, request, name, stage):
        flavor = request.GET.get('flavor', 'AC')
        if flavor == AC_FLAVOR:
            configs = environs_helper.get_env_agent_config(request, name, stage)
        else:
            configs = environs_helper.get_env_script_config(request, name, stage)
        if request.is_ajax():
            # return data for ajax calls
            env = environs_helper.get_env_by_stage(request, name, stage)
            html = render_to_string('configs/config_map.tmpl', {
                "env": env,
                "configs": configs,
                "customization": CUSTOMIZATION[flavor],
                "csrf_token": get_token(request),
            })
            return HttpResponse(json.dumps({'html': html}), content_type="application/json")

        # otherwise, return a page
        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = get_all_stages(envs, stage)

        return render(request, 'configs/config_map.html', {
            "envs": envs,
            "env": env,
            "all_stage_types": sorted(environs_helper.STAGE_TYPES),
            "stages": stages,
            "configs": configs,
            "customization": CUSTOMIZATION[flavor],
        })

    def parse_configs(self, query_dict):
        configs = {}
        for key, value in query_dict.items():
            if not value:
                continue
            if key.startswith('TELETRAAN_'):
                name = key[len('TELETRAAN_'):]
                configs[name] = value
        return configs

    def post(self, request, name, stage):
        configs = self.parse_configs(request.POST)
        flavor = request.POST.get('flavor', 'AC')
        if flavor == AC_FLAVOR:
            environs_helper.update_env_agent_config(request, name, stage, data=configs)
            configs = environs_helper.get_env_agent_config(request, name, stage)
        else:
            environs_helper.update_env_script_config(request, name, stage, data=configs)
            configs = environs_helper.get_env_script_config(request, name, stage)
        env = environs_helper.get_env_by_stage(request, name, stage)
        html = render_to_string('configs/config_map.tmpl', {
            "env": env,
            "configs": configs,
            "customization": CUSTOMIZATION[flavor],
            "csrf_token": get_token(request),
        })
        return HttpResponse(json.dumps({'html': html}), content_type="application/json")
