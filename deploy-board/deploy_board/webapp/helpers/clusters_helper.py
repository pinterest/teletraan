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


class StatefulStatuses:
    UNKNOWN = None
    STATEFUL = True
    STATELESS = False

    @classmethod
    def get_status(cls, status):
        for key, value in vars(cls).items():
            if status == value:
                return key
            elif status == key:
                return value

    @classmethod
    def get_all_statuses(cls):
        return [
            item
            for item in cls.__dict__
            if not callable(getattr(cls, item)) and not item.startswith("__")
        ]


rodimus_client = RodimusClient()


def create_cluster(request, cluster_name, cluster_info):
    return rodimus_client.post(
        "/clusters/%s" % cluster_name,
        request.teletraan_user_id.token,
        data=cluster_info,
    )


def create_cluster_with_env(request, cluster_name, env_name, stage_name, cluster_info):
    return rodimus_client.post(
        "/clusters/%s/%s/%s" % (cluster_name, env_name, stage_name),
        request.teletraan_user_id.token,
        data=cluster_info,
    )


def update_cluster(request, cluster_name, cluster_info):
    return rodimus_client.put(
        "/clusters/%s" % cluster_name,
        request.teletraan_user_id.token,
        data=cluster_info,
    )


def get_cluster(request, cluster_name):
    if cluster_name:
        return rodimus_client.get(
            "/clusters/%s" % cluster_name, request.teletraan_user_id.token
        )
    else:
        return None


def delete_cluster(request, cluster_name):
    return rodimus_client.delete(
        "/clusters/%s" % cluster_name, request.teletraan_user_id.token
    )


def get_host_ids(request, cluster_name):
    return rodimus_client.get(
        "/clusters/%s/hosts" % cluster_name, request.teletraan_user_id.token
    )


def launch_hosts(request, cluster_name, num):
    params = [("num", num)]
    return rodimus_client.put(
        "/clusters/%s/hosts" % cluster_name,
        request.teletraan_user_id.token,
        params=params,
    )


def force_terminate_hosts(request, cluster_name, host_ids, replace_host):
    params = [("replaceHost", replace_host)]
    return rodimus_client.delete(
        "/clusters/%s/hosts" % cluster_name,
        request.teletraan_user_id.token,
        params=params,
        data=host_ids,
    )


def get_cluster_replacement_status(request, data):
    return rodimus_client.post(
        "/cluster-replacements/status", request.teletraan_user_id.token, data=data
    )


def start_cluster_replacement(request, data):
    return rodimus_client.put(
        "/cluster-replacements", request.teletraan_user_id.token, data=data
    )


def get_cluster_auto_refresh_config(request, cluster_name):
    return rodimus_client.get(
        "/cluster-replacements/auto-refresh/config/%s" % cluster_name,
        request.teletraan_user_id.token,
    )


def get_default_cluster_auto_refresh_config(request, cluster_name):
    return rodimus_client.get(
        "/cluster-replacements/auto-refresh/config/default/%s" % cluster_name,
        request.teletraan_user_id.token,
    )


def submit_cluster_auto_refresh_config(request, data):
    return rodimus_client.post(
        "/cluster-replacements/auto-refresh/config",
        request.teletraan_user_id.token,
        data=data,
    )


def perform_cluster_replacement_action(request, cluster_name, action):
    if action == "cancel":
        return rodimus_client.put(
            "/cluster-replacements/action/{}".format(cluster_name),
            request.teletraan_user_id.token,
            params=[("action", "CANCEL")],
        )
    elif action == "pause":
        return rodimus_client.put(
            "/cluster-replacements/action/{}".format(cluster_name),
            request.teletraan_user_id.token,
            params=[("action", "PAUSE")],
        )
    elif action == "resume":
        return rodimus_client.put(
            "/cluster-replacements/action/{}".format(cluster_name),
            request.teletraan_user_id.token,
            params=[("action", "RESUME")],
        )


def update_cluster_capacity(request, cluster_name, min_size, max_size):
    params = [("minsize", min_size), ("maxsize", max_size)]
    return rodimus_client.put(
        "/clusters/%s/capacity" % cluster_name,
        request.teletraan_user_id.token,
        params=params,
    )
