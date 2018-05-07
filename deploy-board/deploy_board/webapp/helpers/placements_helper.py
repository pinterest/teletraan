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

from deploy_board.webapp.helpers.rodimus_client import RodimusClient

rodimus_client = RodimusClient()


def create_placement(request, placement_info):
    return rodimus_client.post("/placements", request.teletraan_user_id.token, data=placement_info)


def get_all(request, index, size):
    params = [('pageIndex', index), ('pageSize', size)]
    return rodimus_client.get("/placements", request.teletraan_user_id.token, params=params)


def get_by_provider_and_cell_name(request, provider, cell_name):
    if cell_name:
        return rodimus_client.get("/placements/cell/%s" % cell_name, request.teletraan_user_id.token)
    return rodimus_client.get("/placements/provider/%s" % provider, request.teletraan_user_id.token)


def get_by_id(request, placement_id):
    return rodimus_client.get("/placements/%s" % placement_id, request.teletraan_user_id.token)


def get_by_cell_name_and_security_group_id(request, cell_name, security_group_id):
    params = [('cellName', cell_name), ('securityGroupId', security_group_id)]
    return rodimus_client.get("/placements", request.teletraan_user_id.token, params=params)
