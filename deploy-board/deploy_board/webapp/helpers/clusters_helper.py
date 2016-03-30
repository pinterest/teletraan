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


def create_cluster(request, env_name, stage_name, cluster_info):
    return deploy_client.post("/envs/%s/%s/clusters" % (env_name, stage_name), request.teletraan_user_id.token,
                              data=cluster_info)


def update_cluster(request, env_name, stage_name, cluster_info):
    return deploy_client.put("/envs/%s/%s/clusters" % (env_name, stage_name), request.teletraan_user_id.token,
                             data=cluster_info)


def get_cluster(request, env_name, stage_name):
    return deploy_client.get("/envs/%s/%s/clusters" % (env_name, stage_name), request.teletraan_user_id.token)


def delete_cluster(request, env_name, stage_name):
    return deploy_client.delete("/envs/%s/%s/clusters" % (env_name, stage_name), request.teletraan_user_id.token)


def create_advanced_cluster(request, env_name, stage_name, provider, cluster_info):
    return deploy_client.post("/envs/%s/%s/clusters/provider/%s" % (env_name, stage_name, provider),
                              request.teletraan_user_id.token, data=cluster_info)


def update_advanced_cluster(request, env_name, stage_name, provider, cluster_info):
    return deploy_client.put("/envs/%s/%s/clusters/provider/%s" % (env_name, stage_name, provider),
                             request.teletraan_user_id.token, data=cluster_info)


def get_advanced_cluster(request, env_name, stage_name, provider):
    return deploy_client.get("/envs/%s/%s/clusters/provider/%s" % (env_name, stage_name, provider),
                             request.teletraan_user_id.token)


def get_hosts(request, env_name, stage_name, host_ids):
    return deploy_client.get("/envs/%s/%s/clusters/hosts" % (env_name, stage_name), request.teletraan_user_id.token,
                             data=host_ids)


def launch_hosts(request, env_name, stage_name, num):
    params = [('num', num)]
    return deploy_client.put("/envs/%s/%s/clusters/hosts" % (env_name, stage_name), request.teletraan_user_id.token,
                             params=params)


def terminate_hosts(request, env_name, stage_name, host_ids, replace_host):
    params = [('replaceHost', replace_host)]
    return deploy_client.delete("/envs/%s/%s/clusters/hosts" % (env_name, stage_name), request.teletraan_user_id.token,
                                data=host_ids, params=params)
