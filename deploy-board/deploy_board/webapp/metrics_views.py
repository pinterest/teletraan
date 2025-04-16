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
"""Collection of all env related views"""

import json
import re
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.template.loader import render_to_string
from django.views.generic import View
import unicodedata
from . import common
from .helpers import environs_helper


class EnvMetricsView(View):
    def get(self, request, name, stage):
        if request.is_ajax():
            env = environs_helper.get_env_by_stage(request, name, stage)
            metrics = environs_helper.get_env_metrics_config(request, name, stage)
            html = render_to_string(
                "configs/metrics_config.tmpl",
                {
                    "env": env,
                    "metrics": metrics,
                    "csrf_token": get_token(request),
                },
            )
            return HttpResponse(
                json.dumps({"html": html}), content_type="application/json"
            )

        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        metrics = environs_helper.get_env_metrics_config(request, name, stage)

        return render(
            request,
            "configs/metrics_config.html",
            {
                "envs": envs,
                "env": env,
                "all_stage_types": sorted(environs_helper.STAGE_TYPES),
                "stages": stages,
                "metrics": metrics,
            },
        )

    def _slugify(self, text):
        text = (
            unicodedata.normalize("NFKD", text)
            .encode("ascii", "ignore")
            .decode("ascii")
        )
        text = re.sub("[^\w\s-]", "", text).strip().lower()
        return re.sub("[-\s]+", "-", text)

    def _parse_metrics_configs(self, query_data):
        page_data = dict(query_data.lists())
        configs = []
        for key, value in page_data.items():
            if not value:
                continue
            if key.startswith("TELETRAAN_"):
                metricsConfig = {}
                name = key[len("TELETRAAN_") :]
                slugified_name = self._slugify(name)
                metricsConfig["title"] = name
                metricsConfig["url"] = value[0]
                min_string = "min_%s" % slugified_name
                max_string = "max_%s" % slugified_name
                color_string = "color-selection_%s" % slugified_name
                metricsSpecs = []

                if min_string in list(page_data.keys()):
                    num_specs = len(page_data[min_string])
                    for i in range(num_specs):
                        spec = {}
                        spec["min"] = float(page_data[min_string][i])
                        spec["max"] = float(page_data[max_string][i])
                        spec["color"] = page_data[color_string][i]
                        metricsSpecs.append(spec)
                metricsConfig["specs"] = metricsSpecs
                configs.append(metricsConfig)
        return configs

    def post(self, request, name, stage):
        metrics = self._parse_metrics_configs(request.POST)
        environs_helper.update_env_metrics_config(request, name, stage, metrics)
        return self.get(request, name, stage)
