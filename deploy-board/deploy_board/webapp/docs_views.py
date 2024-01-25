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

from django.views.generic import View
from django.shortcuts import render

import os


class SwaggerUIView(View):
    def get(self, request):
        teletraanSwaggerUrl = os.environ.get(
            "TELETRAAN_SERVICE_URL", "http://localhost:8011"
        )
        rodimusSwaggerUrl = os.environ.get(
            "RODIMUS_SERVICE_URL", "http://localhost:8012"
        )
        envStage = os.environ.get("ENV_STAGE", "local")
        if envStage != "local":
            teletraanSwaggerUrl = teletraanSwaggerUrl.replace("http://", "https://")
            rodimusSwaggerUrl = rodimusSwaggerUrl.replace("http://", "https://")
        response = render(
            request,
            "swagger-ui/dist/index.html",
            {
                "token": request.session.get("oauth_token"),
                "envStage": envStage,
                "teletraanSwaggerUrl": teletraanSwaggerUrl,
                "rodimusSwaggerUrl": rodimusSwaggerUrl,
            },
        )
        return response
