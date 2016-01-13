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
from deploy_board.webapp.helpers.deployclient import DeployClient

deployclient = DeployClient()


def get_asg_size_metric(request, group_name, start):
    params = {"groupName": group_name, "start": start}
    try:
        return deployclient.get("/metrics/autoscaling/groups/size", request.teletraan_user_id.token,
                                params=params)
    except:
        return []


def get_metric_data(request, group_name, metric_name, start):
    params = {"groupName": group_name, "metricName": metric_name, "start": start}
    try:
        return deployclient.get("/metrics/autoscaling/groups", request.teletraan_user_id.token,
                                params=params)
    except:
        return []


def get_latency_data(request, env_id, type, start):
    params = {"envId": env_id, "type": type, "start": start}
    try:
        return deployclient.get("/metrics/autoscaling/latency", request.teletraan_user_id.token,
                                params=params)
    except:
        return []


def get_raw_metrics(request, metric_name, start):
    params = {"metricName": metric_name, "start": start}
    try:
        return deployclient.get("/metrics/autoscaling/raw_metrics", request.teletraan_user_id.token, params=params)
    except:
        return []
