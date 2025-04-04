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
"""Collection of all hotfix related calls"""

from deploy_board.webapp.helpers.deployclient import DeployClient

deployclient = DeployClient()


def get(request, id):
    return deployclient.get("/hotfixs/%s" % id, request.teletraan_user_id.token)


def get_all(request, envName, index=1, size=30):
    params = [("envName", envName), ("pageIndex", index), ("pageSize", size)]
    return deployclient.get("/hotfixs", request.teletraan_user_id.token, params=params)


def create(request, envName, baseDeployId, commits):
    payload = {"envName": envName, "baseDeployId": baseDeployId, "commits": commits}
    return deployclient.post("/hotfixs", request.teletraan_user_id.token, data=payload)


def is_user_eligible(request, user_name):
    return deployclient.get(
        "/ratings/%s/is_eligible" % user_name, request.teletraan_user_id.token
    )
