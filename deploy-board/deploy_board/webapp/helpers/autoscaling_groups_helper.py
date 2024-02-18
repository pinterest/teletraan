# -*- coding: utf-8 -*-

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

from deploy_board.webapp.helpers.rodimus_client import RodimusClient

rodimus_client = RodimusClient()


TerminationPolicy = ["Default", "OldestInstance", "NewestInstance", "ClosestToNextInstanceHour"]

Comparator = [{"value": "GreaterThanOrEqualToThreshold", "symbol": "≥"},
              {"value": "GreaterThanThreshold", "symbol": ">"},
              {"value": "LessThanOrEqualToThreshold", "symbol": "≤"},
              {"value": "LessThanThreshold", "symbol": "<"}]


#
# Groups resource
#
def get_env_group_names(request, start, size, name_filter=None):
    params = [('start', start), ('size', size)]
    if name_filter:
        params.append(('nameFilter', name_filter))
    return rodimus_client.get("/groups/names", request.teletraan_user_id.token, params=params)


def create_launch_config(request, group_name, asg_info):
    return rodimus_client.post("/groups/%s" % group_name, request.teletraan_user_id.token, data=asg_info)


def update_launch_config(request, group_name, asg_info):
    return rodimus_client.put("/groups/%s" % group_name, request.teletraan_user_id.token, data=asg_info)


def update_group_info(request, group_name, group_info):
    return rodimus_client.put("/groups/%s/config" % group_name, request.teletraan_user_id.token, data=group_info)


def get_group_info(request, group_name):
    return rodimus_client.get("/groups/%s" % group_name, request.teletraan_user_id.token)


def launch_hosts(request, group_name, host_count, subnet):
    params = {"hostCount": host_count, "subnet": subnet}
    return rodimus_client.put("/groups/%s/hosts" % group_name, request.teletraan_user_id.token, params=params)


def launch_hosts_with_placement_group(request, group_name, host_count, subnet, placement_group):
    data = {"hostCount": host_count,
            "cloudLaunchConfig": {"subnet": subnet, "placementGroup": placement_group}}

    # Note: this sends a post request while above sends a put
    return rodimus_client.post("/groups/%s/hosts" % group_name, request.teletraan_user_id.token, data=data)


def terminate_all_hosts(request, group_name):
    return rodimus_client.delete("/groups/%s/terminate/all" % group_name, request.teletraan_user_id.token)


# Health Checks
def create_health_check(request, group_name, health_check_info):
    return rodimus_client.post("/groups/%s/healthcheck" % group_name, request.teletraan_user_id.token,
                               data=health_check_info)


def enable_health_check(request, group_name):
    params = [('actionType', 'ENABLE')]
    return rodimus_client.post("/groups/%s/healthcheck/action" % group_name, request.teletraan_user_id.token,
                               params=params)


def disable_health_check(request, group_name):
    params = [('actionType', 'DISABLE')]
    return rodimus_client.post("/groups/%s/healthcheck/action" % group_name, request.teletraan_user_id.token,
                               params=params)


def get_health_check_activities(request, group_name, index, size):
    params = [('pageIndex', index), ('pageSize', size)]
    return rodimus_client.get("/groups/%s/healthchecks/" % group_name, request.teletraan_user_id.token, params=params)


def get_health_check(request, id):
    return rodimus_client.get("/groups/healthchecks/%s" % id, request.teletraan_user_id.token)


def get_health_check_error(request, id):
    return rodimus_client.get("/groups/healthchecks/errors/%s" % id, request.teletraan_user_id.token)


# Config history
def get_config_history(request, group_name, index, size):
    params = [('pageIndex', index), ('pageSize', size)]
    return rodimus_client.get("/groups/%s/configs/history/" % group_name, request.teletraan_user_id.token, params=params)


#
# AutoScalingGroups resource
#
def create_autoscaling(request, cluster_name, asg_info):
    return rodimus_client.post("/clusters/%s/autoscaling" % cluster_name, request.teletraan_user_id.token,
                               data=asg_info)


def update_autoscaling(request, cluster_name, asg_info):
    return rodimus_client.put("/clusters/%s/autoscaling" % cluster_name, request.teletraan_user_id.token, data=asg_info)


def delete_autoscaling(request, cluster_name, detach_host):
    params = [('detachHosts', detach_host)]
    return rodimus_client.delete("/clusters/%s/autoscaling" % cluster_name, request.teletraan_user_id.token,
                                 params=params)


def get_autoscaling(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling" % cluster_name, request.teletraan_user_id.token)


def get_autoscaling_summary(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/summary" % cluster_name, request.teletraan_user_id.token)


# Asg Actions
def get_autoscaling_status(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/status" % cluster_name, request.teletraan_user_id.token)


def enable_autoscaling(request, cluster_name):
    params = [('actionType', 'ENABLE')]
    return rodimus_client.post("/clusters/%s/autoscaling/action" % cluster_name, request.teletraan_user_id.token,
                               params=params)


def disable_autoscaling(request, cluster_name):
    params = [('actionType', 'DISABLE')]
    return rodimus_client.post("/clusters/%s/autoscaling/action" % cluster_name, request.teletraan_user_id.token,
                               params=params)


def disable_scaling_down_event(request, cluster_name):
    params = [('actionType', 'DISABLE_TERMINATE')]
    return rodimus_client.post("/clusters/%s/autoscaling/action" % cluster_name, request.teletraan_user_id.token,
                               params=params)


def get_disabled_asg_actions(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/action" % cluster_name, request.teletraan_user_id.token)

def update_scaling_process(request, cluster_name, update_request):
    return rodimus_client.post("/clusters/%s/autoscaling/processes" % cluster_name, request.teletraan_user_id.token,
                               data=update_request)

def get_available_scaling_process(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/available-processes" % cluster_name, request.teletraan_user_id.token)

# Asg Alarms
def put_scaling_policies(request, cluster_name, policies_info):
    return rodimus_client.post("/clusters/%s/autoscaling/policies" % cluster_name, request.teletraan_user_id.token,
                               data=policies_info)

def delete_scaling_policy(request, cluster_name, policy_name):
    return rodimus_client.delete("/clusters/%s/autoscaling/policies/%s" % (cluster_name, policy_name), request.teletraan_user_id.token)


def get_policies(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/policies" % cluster_name, request.teletraan_user_id.token)


def add_alarm(request, cluster_name, alarm_infos):
    return rodimus_client.post("/clusters/%s/autoscaling/alarms" % cluster_name, request.teletraan_user_id.token,
                               data=alarm_infos)


def update_alarms(request, cluster_name, alarm_infos):
    return rodimus_client.put("/clusters/%s/autoscaling/alarms" % cluster_name, request.teletraan_user_id.token,
                              data=alarm_infos)


def get_alarms(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/alarms" % cluster_name, request.teletraan_user_id.token)


def delete_alarm(request, cluster_name, alarm_id):
    return rodimus_client.delete("/clusters/%s/autoscaling/alarms/%s" % (cluster_name, alarm_id),
                                 request.teletraan_user_id.token)


def get_alarm_state(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/alarmstate" % cluster_name, request.teletraan_user_id.token)


def get_system_metrics(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/metrics/system" % cluster_name, request.teletraan_user_id.token)


# Asg Schedules
def add_scheduled_actions(request, cluster_name, schedule_actions):
    return rodimus_client.post("/clusters/%s/autoscaling/schedules" % cluster_name, request.teletraan_user_id.token,
                               data=schedule_actions)


def delete_scheduled_action(request, cluster_name, action_id):
    return rodimus_client.delete("/clusters/%s/autoscaling/schedules/%s" % (cluster_name, action_id),
                                 request.teletraan_user_id.token)


def get_scheduled_actions(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/schedules" % cluster_name,
                              request.teletraan_user_id.token)


# Scaling activities
def get_scaling_activities(request, cluster_name, page_size, token):
    params = {"size": page_size, "token": token}
    return rodimus_client.get("/clusters/%s/autoscaling/activities" % cluster_name, request.teletraan_user_id.token,
                              params=params)


# pas
def update_pas_config(request, cluster_name, pas_config):
    return rodimus_client.put("/clusters/%s/autoscaling/pas" % cluster_name, request.teletraan_user_id.token,
                              data=pas_config)


def get_pas_config(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/pas" % cluster_name, request.teletraan_user_id.token)


# hosts
# TODO no usage
def get_autoscaling_group_hosts(request, cluster_name):
    return rodimus_client.get("/clusters/%s/autoscaling/hosts" % cluster_name, request.teletraan_user_id.token)


def hosts_action_in_group(request, cluster_name, host_ids, action):
    params = {"actionType": action}
    return rodimus_client.post("/clusters/%s/autoscaling/hosts/action" % cluster_name, request.teletraan_user_id.token,
                               params=params, data=host_ids)


def is_hosts_protected(request, cluster_name, host_ids):
    return rodimus_client.get("/clusters/%s/autoscaling/host/protection" % cluster_name,
                              request.teletraan_user_id.token, data=host_ids)
