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

deployclient = DeployClient()


def get_hosts_by_name(request, host_name):
    return deployclient.get("/hosts/%s" % host_name, request.teletraan_user_id.token)

def get_accout_id_by_name(request, host_name):
    return deployclient.get("/%s/accountId" % host_name, request.teletraan_user_id.token)