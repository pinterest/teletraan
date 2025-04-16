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
"""Collection of all launch spec related calls"""

from deploy_board.webapp.helpers.rodimus_client import RodimusClient

rodimus_client = RodimusClient()


def get_instance_types(request):
    return rodimus_client.get("/specs/instance_types", request.teletraan_user_id.token)


def get_security_groups(request):
    return rodimus_client.get("/specs/security_groups", request.teletraan_user_id.token)


def get_subnets(request):
    return rodimus_client.get("/specs/subnets", request.teletraan_user_id.token)
