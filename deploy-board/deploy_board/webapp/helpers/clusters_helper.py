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


def create_cluster(request, cluster_name, cluster_info):
    return rodimus_client.post("/clusters/%s" % cluster_name, request.teletraan_user_id.token, data=cluster_info)


def update_cluster(request, cluster_name, cluster_info):
    return rodimus_client.put("/clusters/%s" % cluster_name, request.teletraan_user_id.token, data=cluster_info)


def get_cluster(request, cluster_name):
    if cluster_name:
        return rodimus_client.get("/clusters/%s" % cluster_name, request.teletraan_user_id.token)
    else:
        return None


def delete_cluster(request, cluster_name):
    return rodimus_client.delete("/clusters/%s" % cluster_name, request.teletraan_user_id.token)


def get_host_ids(request, cluster_name):
    return rodimus_client.get("/clusters/%s/hosts" % cluster_name, request.teletraan_user_id.token)


def launch_hosts(request, cluster_name, num):
    params = [('num', num)]
    return rodimus_client.put("/clusters/%s/hosts" % cluster_name, request.teletraan_user_id.token, params=params)


def force_terminate_hosts(request, cluster_name, host_ids, replace_host):
    params = [('replaceHost', replace_host)]
    return rodimus_client.delete("/clusters/%s/hosts" % cluster_name, request.teletraan_user_id.token, params=params,
                                 data=host_ids)


def enable_cluster_replacement(request, cluster_name):
    params = [('actionType', 'REPLACE')]
    return rodimus_client.put("/clusters/%s/actions" % cluster_name, request.teletraan_user_id.token, params=params)


def pause_cluster_replacement(request, cluster_name):
    params = [('actionType', 'PAUSE_REPLACE')]
    return rodimus_client.put("/clusters/%s/actions" % cluster_name, request.teletraan_user_id.token, params=params)


def resume_cluster_replacement(request, cluster_name):
    params = [('actionType', 'RESUME_REPLACE')]
    return rodimus_client.put("/clusters/%s/actions" % cluster_name, request.teletraan_user_id.token, params=params)


def cancel_cluster_replacement(request, cluster_name):
    params = [('actionType', 'CANCEL_REPLACE')]
    return rodimus_client.put("/clusters/%s/actions" % cluster_name, request.teletraan_user_id.token, params=params)
