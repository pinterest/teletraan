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
"""Collection of all alarm config related views"""

import json
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.template.loader import render_to_string
from django.views.generic import View
from . import common
from .helpers import environs_helper


class EnvAlarmView(View):
    def get(self, request, name, stage):
        if request.is_ajax():
            env = environs_helper.get_env_by_stage(request, name, stage)
            alarms = environs_helper.get_env_alarms_config(request, name, stage)
            html = render_to_string(
                "configs/alarm_config.tmpl",
                {
                    "env": env,
                    "alarms": alarms,
                    "csrf_token": get_token(request),
                },
            )
            return HttpResponse(
                json.dumps({"html": html}), content_type="application/json"
            )

        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        alarms = environs_helper.get_env_alarms_config(request, name, stage)

        return render(
            request,
            "configs/alarm_config.html",
            {
                "envs": envs,
                "env": env,
                "all_stage_types": sorted(environs_helper.STAGE_TYPES),
                "stages": stages,
                "alarms": alarms,
            },
        )

    def post(self, request, name, stage):
        alarms = []
        for key, value in request.POST.items():
            if not value:
                continue
            if key.startswith("TELETRAAN_"):
                alarm = {}
                alarm["name"] = key[len("TELETRAAN_") :]
                alarm["alarmUrl"] = value
                alarms.append(alarm)

        environs_helper.update_env_alarms_config(request, name, stage, alarms)
        return self.get(request, name, stage)
