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

from deploy_board.webapp.helpers.deployclient import DeployClient

deploy_client = DeployClient()


def create_base_image(request, base_image_info):
    return deploy_client.post("/base_images", request.teletraan_user_id.token, data=base_image_info)


def get_all(request, index, size):
    params = [('pageIndex', index), ('pageSize', size)]
    return deploy_client.get("/base_images", request.teletraan_user_id.token, params=params)


def get_image_names(request, provider):
    params = [('provider', provider)]
    return deploy_client.get("/base_images/names", request.teletraan_user_id.token, params=params)


def get_by_name(request, name):
    return deploy_client.get("/base_images/names/%s" % name, request.teletraan_user_id.token)


def get_by_id(request, id):
    return deploy_client.get("/base_images/%s" % id, request.teletraan_user_id.token)


def get_all_providers(request):
    return deploy_client.get("/base_images/provider", request.teletraan_user_id.token)