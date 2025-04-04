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
"""Collection of all rating related calls"""

from deploy_board.webapp.helpers.deployclient import DeployClient

deployclient = DeployClient()


def get_ratings(request, index, size):
    return deployclient.get("/ratings", request.teletraan_user_id.token)


def create_ratings(request, rating, feedback):
    payload = {"rating": rating, "feedback": feedback}
    return deployclient.post("/ratings", request.teletraan_user_id.token, data=payload)


def is_user_eligible(request, user_name):
    return deployclient.get(
        "/ratings/%s/is_eligible" % user_name, request.teletraan_user_id.token
    )
