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
"""Collection of all deploy scheduling config related views
"""
import json
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.template.loader import render_to_string
from django.views.generic import View
from . import common
from .helpers import environs_helper, agents_helper, schedules_helper


class EnvScheduleView(View):
    def get(self, request, name, stage):
        if request.is_ajax():
            env = environs_helper.get_env_by_stage(request, name, stage)
            html = render_to_string('configs/schedule_config.tmpl', {
                "env": env,
                "csrf_token": get_token(request),
            })
            return HttpResponse(json.dumps({'html': html}), content_type="application/json")

        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        agent_count = agents_helper.get_agents_total_by_env(request, env["id"])
        schedule_id = env.get('scheduleId', None)
        if schedule_id is not None:
            schedule = schedules_helper.get_schedule(request, name, stage, schedule_id)
        else:
            schedule = None
        max_parallel_number = env["maxParallel"]
        return render(request, 'configs/schedule_config.html', {
            "env": env,
            "schedule": schedule,
            "agent_count": agent_count,
            "max_parallel_number": max_parallel_number,
        })
