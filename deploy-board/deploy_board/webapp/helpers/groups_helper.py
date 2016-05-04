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
"""Collection of all groups related calls
"""
from deploy_board.webapp.helpers.deployclient import DeployClient


TerminationPolicy = ["Default", "OldestInstance", "NewestInstance",
                     "OldestLaunchConfiguration", "ClosestToNextInstanceHour"]

Comparator = ["GreaterThanOrEqualToThreshold", "GreaterThanThreshold",
              "LessThanOrEqualToThreshold", "LessThanThreshold"]

deployclient = DeployClient()


def get_env_group_names(request, start, size):
    params = {"start": start, "size": size}
    return deployclient.get("/groups/names", request.teletraan_user_id.token, params=params)


def create_group(request, group_name):
    return deployclient.post("/groups/{}".format(group_name), request.teletraan_user_id.token)


def get_group_instances(request, group_name):
    return deployclient.get("/groups/{}/instances".format(group_name),
                            request.teletraan_user_id.token)


def disable_autoscaling(request, group_name):
    return deployclient.post("/groups/{}/autoscaling/action".format(group_name),
                             request.teletraan_user_id.token, params={"type": "disable"})


def enable_autoscaling(request, group_name):
    return deployclient.post("/groups/{}/autoscaling/action".format(group_name),
                             request.teletraan_user_id.token, params={"type": "enable"})


def create_autoscaling(request, group_name, asg_info):
    return deployclient.post("/groups/{}/autoscaling".format(group_name),
                             request.teletraan_user_id.token,
                             data=asg_info)


def delete_autoscaling(request, group_name, detach_instance):
    return deployclient.delete("/groups/{}/autoscaling".format(group_name),
                               request.teletraan_user_id.token,
                               params={"detach_instances": detach_instance})


def get_autoscaling(request, group_name):
    return deployclient.get("/groups/{}/autoscaling".format(group_name),
                            request.teletraan_user_id.token)


def get_autoscaling_summary(request, group_name):
    return deployclient.get("/groups/{}/autoscaling/summary".format(group_name),
                            request.teletraan_user_id.token)


def get_autoscaling_group_instances(request, group_name):
    return deployclient.get("/groups/{}/autoscaling/instances".format(group_name),
                            request.teletraan_user_id.token)


def update_autoscaling(request, group_name, asg_info):
    return deployclient.put("/groups/{}/autoscaling".format(group_name),
                            request.teletraan_user_id.token,
                            data=asg_info)


def get_autoscaling_status(request, group_name):
    return deployclient.get("/groups/{}/autoscaling/status".format(group_name),
                            request.teletraan_user_id.token)


def get_group_info(request, group_name):
    return deployclient.get("/groups/{}".format(group_name), request.teletraan_user_id.token)


def update_group_info(request, group_name, group_info):
    return deployclient.put("/groups/{}/".format(group_name), request.teletraan_user_id.token,
                            data=group_info)


def get_policies(request, group_name):
    return deployclient.get("/groups/{}/autoscaling/policies".format(group_name),
                            request.teletraan_user_id.token)


def put_scaling_policies(request, group_name, policies_info):
    return deployclient.post("/groups/{}/autoscaling/policies".format(group_name),
                             request.teletraan_user_id.token, data=policies_info)


def get_scaling_activities(request, group_name, page_size, token):
    params = {"size": page_size, "token": token}
    return deployclient.get("/groups/{}/autoscaling/activities".format(group_name),
                            request.teletraan_user_id.token, params=params)


# alarms
def get_alarms(request, group_name):
    return deployclient.get("/groups/{}/autoscaling/alarms".format(group_name),
                            request.teletraan_user_id.token)


def update_alarms(request, group_name, alarm_infos):
    return deployclient.put("/groups/{}/autoscaling/alarms".format(group_name),
                            request.teletraan_user_id.token, data=alarm_infos)


def create_alarms(request, group_name, alarm_infos):
    return deployclient.post("/groups/{}/autoscaling/alarms".format(group_name),
                             request.teletraan_user_id.token, data=alarm_infos)


def delete_alarm(request, group_name, alarm_id):
    return deployclient.delete("/groups/{}/autoscaling/alarms/{}".format(group_name, alarm_id),
                               request.teletraan_user_id.token)


def add_alarm(request, group_name, alarm_infos):
    return deployclient.post("/groups/{}/autoscaling/alarms".format(group_name),
                             request.teletraan_user_id.token, data=alarm_infos)


# get group host information
def get_host_info(request, group_name):
    return deployclient.get("/groups/{}/hosts/".format(group_name),
                            request.teletraan_user_id.token)


def get_system_metrics(request, group_name):
    return deployclient.get("/groups/{}/autoscaling/metrics/system".format(group_name),
                            request.teletraan_user_id.token)


# launch host
def launch_instance_in_group(request, group_name, instance_count, subnet):
    params = {"instanceCount": instance_count, "subnet": subnet}
    return deployclient.put("/groups/{}/instances".format(group_name),
                            request.teletraan_user_id.token, params=params)


def protect_instance_in_group(request, group_name, instance_ids):
    return instance_action_in_group(request, group_name, instance_ids, "protect")


def unprotect_instance_in_group(request, group_name, instance_ids):
    return instance_action_in_group(request, group_name, instance_ids, "unprotect")


def instance_action_in_group(request, group_name, instance_ids, action):
    params = {"type": action}
    return deployclient.post("/groups/{}/autoscaling/instances/action".format(group_name),
                             request.teletraan_user_id.token, params=params, data=instance_ids)


def is_instance_protected(request, group_name, instance_id):
    return deployclient.get("/groups/{}/autoscaling/instance/protection".format(group_name),
                            request.teletraan_user_id.token, data=instance_id)

# config history
def get_config_history(request, group_name, index, size):
    params = [('pageIndex', index), ('pageSize', size)]
    return deployclient.get("/groups/{}/configs/history/".format(group_name),
                            request.teletraan_user_id.token, params=params)


def rollback_config(request, group_name, change_id):
    params = [('actionType', 'ROLLBACK'), ('changeId', change_id)]
    return deployclient.put("/groups/{}/configs/actions".format(group_name),
                            request.teletraan_user_id.token, params=params)


# Health Check
def get_health_check_activities(request, group_name, index, size):
    params = [('pageIndex', index), ('pageSize', size)]
    return deployclient.get("/groups/{}/healthchecks/".format(group_name),
                            request.teletraan_user_id.token, params=params)


def get_health_check(request, id):
    return deployclient.get("/groups/healthchecks/{}".format(id), request.teletraan_user_id.token)


def create_health_check(request, group_name, health_check_info):
    return deployclient.post("/groups/{}/healthcheck/".format(group_name), request.teletraan_user_id.token,
                             data=health_check_info)


def get_health_check_error(request, id):
    return deployclient.get("/groups/healthchecks/errors/{}".format(id),
                            request.teletraan_user_id.token)


def enable_health_check(request, group_name):
    return deployclient.post("/groups/{}/healthcheck/action".format(group_name), request.teletraan_user_id.token,
                             params={"type": "enable"})


def disable_health_check(request, group_name):
    return deployclient.post("/groups/{}/healthcheck/action".format(group_name), request.teletraan_user_id.token,
                             params={"type": "disable"})


def get_scaling_down_event_status(request, group_name):
    return deployclient.get("/groups/{}/autoscaling/event/scalingdown".format(group_name),
                            request.teletraan_user_id.token)


def enable_scaling_down_event(request, group_name):
    return deployclient.post("/groups/{}/autoscaling/event/scalingdown/action".format(group_name),
                             request.teletraan_user_id.token, params={"type": "enable"})


def disable_scaling_down_event(request, group_name):
    return deployclient.post("/groups/{}/autoscaling/event/scalingdown/action".format(group_name),
                             request.teletraan_user_id.token, params={"type": "disable"})
