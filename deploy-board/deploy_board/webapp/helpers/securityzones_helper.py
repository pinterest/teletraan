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


def create_security_zone(request, security_zone_info):
    return rodimus_client.post("/security_zones", request.teletraan_user_id.token, data=security_zone_info)


def get_all(request, index, size):
    params = [('pageIndex', index), ('pageSize', size)]
    return rodimus_client.get("/security_zones", request.teletraan_user_id.token, params=params)


def get_by_provider_and_cell_name(request, account_id, provider, cell_name):
    query = f"?accountId={account_id}" if account_id is not None else ""
    if cell_name:
        return rodimus_client.get(
            f"/security_zones/cell/{cell_name}{query}",
            request.teletraan_user_id.token)
    return rodimus_client.get(
        f"/security_zones/provider/{provider}{query}",
        request.teletraan_user_id.token)


def get_by_id(request, security_zone_id):
    return rodimus_client.get("/security_zones/%s" % security_zone_id, request.teletraan_user_id.token)
