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
"""Collection of system level calls"""

from deploy_board.webapp.helpers.deployclient import DeployClient

DEFAULT_NAME_SIZE = 30
deployclient = DeployClient()


def get_url_pattern(request, type=""):
    return deployclient.get(
        "/system/scm_link_template/?scm=" + type, request.teletraan_user_id.token
    )


def get_scm_url(request, type=""):
    return deployclient.get(
        "/system/scm_url/?scm=" + type, request.teletraan_user_id.token
    )["url"]


def send_chat_message(request, from_name, to_name, message):
    data = {"from": from_name, "to": to_name, "message": message}
    return deployclient.post(
        "/system/send_chat_message", request.teletraan_user_id.token, data=data
    )


def ping(request, ping_request):
    return deployclient.post(
        "/system/ping", request.teletraan_user_id.token, data=ping_request
    )


def healthcheck(request):
    return deployclient.get("/healthcheck", request.teletraan_user_id.token)
