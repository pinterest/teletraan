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
import json
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.template.loader import render_to_string
from django.views.generic import View
from . import common
from .helpers import environs_helper


class EnvWebhooksView(View):
    def get(self, request, name, stage):
        if request.is_ajax():
            env = environs_helper.get_env_by_stage(request, name, stage)
            webhooks = environs_helper.get_env_hooks_config(request, name, stage)
            html = render_to_string('configs/webhooks_config.tmpl', {
                "env": env,
                "webhooks": webhooks,
                "csrf_token": get_token(request),
            })
            return HttpResponse(json.dumps({'html': html}), content_type="application/json")

        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        webhooks = environs_helper.get_env_hooks_config(request, name, stage)

        return render(request, 'configs/webhooks_config.html', {
            "envs": envs,
            "env": env,
            "all_stage_types": sorted(environs_helper.STAGE_TYPES),
            "stages": stages,
            "webhooks": webhooks,
        })

    def _parse_webhooks_configs(self, query_data):
        page_data = query_data
        pre_webhooks = []
        post_webhooks = []
        for key, value in page_data.items():
            if key.startswith('url_'):
                label = key.split('_')[1]
                body = "body_%s" % label
                headers = "headers_%s" % label
                deploy_type = "deploy-type_%s" % label
                method = "method_%s" % label
                version = "version_%s" % label
                webhook = {}
                webhook["url"] = value
                webhook['version'] = page_data[version]
                webhook['method'] = page_data[method]
                webhook['body'] = page_data[body].strip()
                webhook['headers'] = page_data[headers]
                type = page_data[deploy_type]
                # Determine whether this is a pre or post webhook
                if type == "post":
                    post_webhooks.append(webhook)
                if type == "pre":
                    pre_webhooks.append(webhook)

        envWebhooks = {}
        envWebhooks['postDeployHooks'] = post_webhooks
        envWebhooks['preDeployHooks'] = pre_webhooks
        return envWebhooks

    def post(self, request, name, stage):
        envWebhooks = self._parse_webhooks_configs(request.POST)
        environs_helper.update_env_hooks_config(request, name, stage, envWebhooks)
        return self.get(request, name, stage)
