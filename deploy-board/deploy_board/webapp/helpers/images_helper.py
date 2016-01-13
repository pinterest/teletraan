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
"""Collection of all host image related calls
"""
from deploy_board.webapp.helpers.deployclient import DeployClient

deploy_client = DeployClient()


def get_all_app_names(request):
    return deploy_client.get("/machine_images/appnames/", request.teletraan_user_id.token)


def get_all_images_by_app(request, appname):
    params = {"app": appname}
    return deploy_client.get("/machine_images/", request.teletraan_user_id.token, params=params)


def get_image_by_id(request, id):
    return deploy_client.get("/machine_images/{}".format(id), request.teletraan_user_id.token)
