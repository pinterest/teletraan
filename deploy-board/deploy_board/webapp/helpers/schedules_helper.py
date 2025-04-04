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
"""Collection of all environs related calls"""

from deploy_board.webapp.helpers.deployclient import DeployClient

deployclient = DeployClient()


def get_schedule(request, envName, stageName, id):
    schedule = deployclient.get(
        "/schedules/%s/%s/%s" % (envName, stageName, id),
        request.teletraan_user_id.token,
    )
    return schedule


def update_schedule(request, envName, stageName, data):
    return deployclient.put(
        "/schedules/%s/%s/schedules" % (envName, stageName),
        request.teletraan_user_id.token,
        data=data,
    )


def override_session(request, envName, stageName, session_num):
    params = [("sessionNumber", session_num)]
    return deployclient.put(
        "/schedules/%s/%s/override" % (envName, stageName),
        request.teletraan_user_id.token,
        params=params,
    )
