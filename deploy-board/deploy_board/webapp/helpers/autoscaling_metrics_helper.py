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
"""Collection of all autoscaling metrics related calls
"""
from deploy_board.webapp.helpers.rodimus_client import RodimusClient

rodimus_client = RodimusClient()


def get_asg_size_metric(request, cluster_name, start):
    params = {"start": start}
    try:
        return rodimus_client.get("/metrics/clusters/%s/size" % cluster_name, request.teletraan_user_id.token,
                                  params=params)
    except Exception:
        return []


def get_metric_data(request, cluster_name, metric_name, start):
    params = {"metricName": metric_name, "start": start}
    try:
        return rodimus_client.get("/metrics/clusters/%s" % cluster_name, request.teletraan_user_id.token, params=params)
    except Exception:
        return []


def get_latency_data(request, env_id, type, start):
    params = {"envId": env_id, "actionType": type, "start": start}
    try:
        return rodimus_client.get("/metrics/latency", request.teletraan_user_id.token, params=params)
    except Exception:
        return []


def get_raw_metrics(request, metric_name, start):
    params = {"metricName": metric_name, "start": start}
    try:
        return rodimus_client.get("/metrics/raw_metrics", request.teletraan_user_id.token, params=params)
    except Exception:
        return []


def get_pas_metrics(request, cluster_name, start, type):
    params = {"clusterName": cluster_name, "start": start, "actionType": type}
    try:
        return rodimus_client.get("/metrics/pas", request.teletraan_user_id.token, params=params)
    except Exception:
        return []
