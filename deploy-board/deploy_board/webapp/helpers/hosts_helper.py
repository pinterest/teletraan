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
"""Collection of all hosts related calls
"""
from deploy_board.webapp.helpers.deployclient import DeployClient
from deploy_board.webapp.helpers.cmdbapiclient import CmdbApiClient
from deploy_board.webapp.helpers.rodimus_client import RodimusClient

deployclient = DeployClient()
cmdbapi_client = CmdbApiClient()
rodimus_client = RodimusClient()

def get_hosts_by_name(request, host_name):
    return deployclient.get("/hosts/%s" % host_name, request.teletraan_user_id.token)

def query_cmdb(query, fields, account_id):
    return cmdbapi_client.query(query, fields, account_id)

def get_cmdb_host_info(host_id, account_id):
    return cmdbapi_client.get_host_details(host_id, account_id)

def get_hosts_is_protected(request, host_ids):
    params = [('hostIds', host_ids)]
    return rodimus_client.post(
        "/hosts/state?actionType=PROTECTED",
        request.teletraan_user_id.token,
        data=host_ids,
    )
