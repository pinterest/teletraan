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


from django.middleware.csrf import get_token
from django.shortcuts import render, redirect
from django.views.generic import View
from django.template.loader import render_to_string
from django.http import HttpResponse
from django.contrib import messages
import json
import logging
import traceback

from helpers import environs_helper, clusters_helper
from helpers import images_helper, groups_helper
from helpers import specs_helper
from helpers import autoscaling_metrics_helper
from diff_match_patch import diff_match_patch
from deploy_board import settings

log = logging.getLogger(__name__)


DEFAULT_PAGE_SIZE = 50

ScalingType = ["ChangeInCapacity", "ExactCapacity", "PercentChangeInCapacity"]

def group_landing(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    group_names = groups_helper.get_env_group_names(request, index, size)
    return render(request, 'groups/group_landing.html', {
        'group_names': group_names,
        "pageIndex": index,
        "pageSize": DEFAULT_PAGE_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(group_names) < DEFAULT_PAGE_SIZE,
    })


def get_system_specs(request):
    instance_types = specs_helper.get_instance_types(request)
    security_groups = specs_helper.get_security_groups(request)
    subnets = specs_helper.get_subnets(request)
    sorted_subnets = sorted(subnets, key=lambda subnet: subnet["info"]["tag"])
    sorted_sgs = sorted(security_groups, key=lambda sg: sg["info"]["name"])
    return instance_types, sorted_subnets, sorted_sgs


def get_launch_config(request, group_name):
    try:
        group_info = groups_helper.get_group_info(request, group_name)
        launch_config = group_info.get("launchInfo")

        if launch_config and launch_config.get("subnets"):
            launch_config["subnetArrays"] = launch_config["subnets"].split(',')
        appNames = images_helper.get_all_app_names(request)
        appNames = sorted(appNames)
        curr_image = images_helper.get_image_by_id(request, launch_config["imageId"])
        html = render_to_string('groups/launch_config.tmpl', {
            "group_name": group_name,
            "app_names": appNames,
            "config": launch_config,
            "curr_image": curr_image,
            "csrf_token": get_token(request),
        })
    except:
        log.error(traceback.format_exc())
        raise
    return HttpResponse(json.dumps(html), content_type="application/json")


def get_group_config_internal(group_config):
    if group_config:
        if group_config.get("launchLatencyTh"):
            group_config["launchLatencyTh"] = group_config.get("launchLatencyTh") / 60
        if group_config.get("healthcheckPeriod"):
            group_config["healthcheckPeriod"] = group_config.get("healthcheckPeriod") / 60
        if group_config.get("lifecycleTimeout"):
            group_config["lifecycleTimeout"] = group_config.get("lifecycleTimeout") / 60
        return group_config
    else:
        group_config = {}
        group_config["launchLatencyTh"] = 10
        group_config["healthcheckPeriod"] = 10
        group_config["lifecycleTimeout"] = 10
        return group_config


def get_group_config(request, group_name):
    try:
        group_info = groups_helper.get_group_info(request, group_name)
        group_config = group_info.get("groupInfo")
        group_config = get_group_config_internal(group_config)
        is_cmp = False
        envs = environs_helper.get_all_envs_by_group(request, group_name)
        for env in envs:
            basic_cluster_info = clusters_helper.get_cluster(request, env.get('envName'), env.get('stageName'))
            if basic_cluster_info:
                is_cmp = True
        html = render_to_string('groups/group_config.tmpl', {
            "group_name": group_name,
            "config": group_config,
            "is_cmp": is_cmp,
            "csrf_token": get_token(request),
        })
    except:
        log.error(traceback.format_exc())
        raise
    return HttpResponse(json.dumps(html), content_type="application/json")


def update_pas_config(request, group_name):
    try:
        params = request.POST
        data = {}
        data['group_name'] = group_name
        data["metric"] = params["metric"]
        data["throughput"] = params["throughput"]
        if "pas_state" in params:
            data["pas_state"] = "enabled"
        else:
            data["pas_state"] = "disabled"

        groups_helper.update_pas_config(request, data)
        return get_pas_config(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise


def get_pas_config(request, group_name):
    try:
        pas_config = groups_helper.get_pas_config(request, group_name)
        html = render_to_string('groups/pase_config.tmpl', {
            "group_name": group_name,
            "pas_config": pas_config
        })
    except:
        log.error(traceback.format_exc())
        raise
    return HttpResponse(json.dumps(html), content_type="application/json")


def update_launch_config(request, group_name):
    try:
        params = request.POST
        launchRequest = {}
        launchRequest["instanceType"] = params["instanceType"]
        launchRequest["securityGroup"] = params["securityGroup"]
        launchRequest["imageId"] = params["imageId"]
        launchRequest["userData"] = params["userData"]
        launchRequest["iamRole"] = params["iamRole"]
        launchRequest["subnets"] = ",".join(params.getlist("subnets"))

        if params["assignPublicIP"] == "True":
            launchRequest["assignPublicIp"] = True
        else:
            launchRequest["assignPublicIp"] = False
        groups_helper.update_launch_config(request, group_name, launchRequest)
        return get_launch_config(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise


def create_launch_config(request, group_name):
    try:
        params = request.POST
        launchRequest = {}
        launchRequest["minSize"] = int(params["minSize"])
        launchRequest["maxSize"] = int(params["maxSize"])
        launchRequest["instanceType"] = params["instanceType"]
        launchRequest["securityGroup"] = params["securityGroup"]
        launchRequest["imageId"] = params["imageId"]
        launchRequest["userData"] = params["userData"]
        launchRequest["iamRole"] = params["iamRole"]
        launchRequest["subnets"] = ",".join(params.getlist("subnets"))
        if params["assignPublicIP"] == "True":
            launchRequest["assignPublicIp"] = True
        else:
            launchRequest["assignPublicIp"] = False
        groups_helper.create_launch_config(request, group_name, launchRequest)
        return redirect("/groups/{}/config".format(group_name))
    except:
        log.error(traceback.format_exc())
        raise


def update_group_config(request, group_name):
    try:
        params = request.POST
        groupRequest = {}
        groupRequest["chatroom"] = params.get("chatroom")
        groupRequest["watchRecipients"] = params.get("watch_recipients")
        groupRequest["emailRecipients"] = params.get("email_recipients")
        groupRequest["pagerRecipients"] = params.get("pager_recipients")
        groupRequest["launchLatencyTh"] = int(params["launch_latency_th"]) * 60
        if "healthcheck_state" in params:
            groupRequest["healthcheckState"] = True
        else:
            groupRequest["healthcheckState"] = False
        groupRequest["healthcheckPeriod"] = int(params["healthcheck_period"]) * 60

        if "lifecycle_state" in params:
            groupRequest["lifecycleState"] = True
        else:
            groupRequest["lifecycleState"] = False
        groupRequest["lifecycleTimeout"] = int(params["lifecycle_timeout"]) * 60
        groups_helper.update_group_info(request, group_name, groupRequest)
        return get_group_config(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise


def disable_asg(request, group_name):
    groups_helper.disable_autoscaling(request, group_name)
    return redirect('/groups/{}'.format(group_name))


def resume_asg(request, group_name):
    groups_helper.enable_autoscaling(request, group_name)
    return redirect('/groups/{}'.format(group_name))


def create_asg(request, group_name):
    params = request.POST
    asgRequest = {}
    asgRequest["groupName"] = group_name
    if "attach_instances" in params:
        asgRequest["attachExistingInstances"] = "true"
    else:
        asgRequest["attachExistingInstances"] = "false"
    asgRequest["minSize"] = int(params["min_size"])
    asgRequest["maxSize"] = int(params["max_size"])
    asgRequest["terminationPolicy"] = params["terminationPolicy"]

    groups_helper.create_autoscaling(request, group_name, asgRequest)
    return redirect('/groups/{}/config/'.format(group_name))


def get_asg_config(request, group_name):
    asg_summary = groups_helper.get_autoscaling_summary(request, group_name)
    instances = groups_helper.get_group_instances(request, group_name)
    group_info = groups_helper.get_group_info(request, group_name)
    launch_config = group_info.get("launchInfo")
    group_size = len(instances)
    policies = groups_helper.TerminationPolicy
    if asg_summary.get("spotRatio", None):
        asg_summary["spotRatio"] *= 100
    if asg_summary.get("sensitivityRatio", None):
        asg_summary["sensitivityRatio"] *= 100
    scheduled_actions = groups_helper.get_scheduled_actions(request, group_name)
    time_based_asg = False
    if len(scheduled_actions) > 0:
        time_based_asg = True
    content = render_to_string("groups/asg_config.tmpl", {
        "group_name": group_name,
        "asg": asg_summary,
        "group_size": group_size,
        "terminationPolicies": policies,
        "instanceType": launch_config.get("instanceType"),
        "time_based_asg": time_based_asg,
        "csrf_token": get_token(request),
    })
    return HttpResponse(json.dumps(content), content_type="application/json")


def delete_asg(request, group_name):
    try:
        params = request.GET
        if params.get("delete") == "1":
            detach_instances = "false"
        else:
            detach_instances = "true"
        groups_helper.delete_autoscaling(request, group_name, detach_instances)
        content = render_to_string("groups/deletion_loading.tmpl",
                                   {"group_name": group_name})
    except:
        raise
    return HttpResponse(json.dumps(content), content_type="application/json")


def get_deleted_asg_status(request, group_name):
    try:
        status = groups_helper.get_autoscaling_status(request, group_name)
        if status == "UNKNOWN":
            asg_status = 0
            log.info("{} is removed from autoscaling group.".format(group_name))
        else:
            asg_status = 1
            log.info("Still waiting for deleting {}".format(group_name))
        content = render_to_string("groups/deletion_loading.tmpl",
                                   {"group_name": group_name})
        return HttpResponse(json.dumps({"content": content, "status": asg_status}), content_type="application/json")
    except:
        raise


def update_asg_config(request, group_name):
    params = request.POST
    try:
        asg_request = {}
        asg_request["groupName"] = group_name
        asg_request["minSize"] = int(params["minSize"])
        asg_request["maxSize"] = int(params["maxSize"])
        asg_request["terminationPolicy"] = params["terminationPolicy"]
        if "enableSpot" in params:
            asg_request["enableSpot"] = True
            asg_request["spotRatio"] = float(params["spotRatio"]) / 100
            asg_request["sensitivityRatio"] = float(params["sensitivityRatio"]) / 100
            asg_request["spotPrice"] = params["bidPrice"]
        else:
            asg_request["enableSpot"] = False
        groups_helper.update_autoscaling(request, group_name, asg_request)
    except:
        log.error(traceback.format_exc())
        raise

    return get_asg_config(request, group_name)


def make_int(s):
    s = s.strip()
    return int(s) if s else 0


class ScalingPolicy(object):
    def __init__(self, policies):
        self.enabled = len(policies.get("scaleupPolicies")) > 0 and \
            len(policies.get("scaledownPolicies")) > 0
        if self.enabled:
            self.scaleUpSize = policies.get("scaleupPolicies")[0].get("scaleSize")
            self.scaleDownSize = policies.get("scaledownPolicies")[0].get("scaleSize")
            self.scaleUpCoolDownTime = policies.get("scaleupPolicies")[0].get("coolDown")
            self.scaleDownCoolDownTime = policies.get("scaledownPolicies")[0].get("coolDown")
            self.scaleUpType = policies.get("scaleupPolicies")[0].get("scalingType")
            self.scaleDownType = policies.get("scaleupPolicies")[0].get("scalingType")


def get_policy(request, group_name):
    policies = groups_helper.get_policies(request, group_name)
    policy = ScalingPolicy(policies)
    content = render_to_string("groups/asg_policy.tmpl", {
        "group_name": group_name,
        "policy": policy,
        "csrf_token": get_token(request),
    })
    return HttpResponse(json.dumps(content), content_type="application/json")


def update_policy(request, group_name):
    params = request.POST
    try:
        scaling_policies = {}
        scaling_policies["scaleupPolicies"] = []
        scaling_policies["scaledownPolicies"] = []
        scaling_policies["scaleupPolicies"].append({"scaleSize": make_int(params["scaleupSize"]),
                                                    "scalingType": params["scaleupType"],
                                                    "coolDown": make_int(params["scaleupCooldownTime"])})
        scaling_policies["scaledownPolicies"].append({"scaleSize": make_int(params["scaledownSize"]),
                                                      "scalingType": params["scaledownType"],
                                                      "coolDown": make_int(params["scaleDownCooldownTime"])})

        groups_helper.put_scaling_policies(request, group_name, scaling_policies)
        return get_policy(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise


# alarm relelated information
def _parse_metrics_configs(query_data, group_name):
    page_data = dict(query_data.lists())
    configs = []
    for key, value in page_data.iteritems():
        if not value:
            continue
        if key.startswith('TELETRAAN_'):
            alarm_info = {}
            alarm_id = key[len('TELETRAAN_'):]
            alarm_info["alarmId"] = alarm_id
            alarm_info["actionType"] = page_data["actionType_{}".format(alarm_id)][0]
            alarm_info["metricSource"] = page_data["metricsUrl_{}".format(alarm_id)][0]
            alarm_info["comparator"] = page_data["comparator_{}".format(alarm_id)][0]
            alarm_info["evaluationTime"] = int(page_data["evaluateTime_{}".format(alarm_id)][0])
            alarm_info["threshold"] = float(page_data["threshold_{}".format(alarm_id)][0])
            if page_data["fromAwsMetric_{}".format(alarm_id)][0] == "True":
                alarm_info["fromAwsMetric"] = True
            else:
                alarm_info["fromAwsMetric"] = False
            alarm_info["groupName"] = group_name
            configs.append(alarm_info)
    return configs


def get_alarms(request, group_name):
    operators = groups_helper.Comparator
    alarms = groups_helper.get_alarms(request, group_name)
    aws_metric_names = groups_helper.get_system_metrics(request, group_name)
    content = render_to_string("groups/asg_metrics.tmpl", {
        "group_name": group_name,
        "alarms": alarms,
        "aws_metric_names": aws_metric_names,
        "csrf_token": get_token(request),
        "comparators": operators,
    })
    return HttpResponse(json.dumps(content), content_type="application/json")


def update_alarms(request, group_name):
    try:
        configs = _parse_metrics_configs(request.POST, group_name)
        groups_helper.update_alarms(request, group_name, configs)
        return get_alarms(request, group_name)
    except:
        log.error(traceback.format_exc())
        return HttpResponse(json.dumps({'content': ""}), content_type="application/json")


def delete_alarms(request, group_name):
    params = request.POST
    alarm_id = params["alarmId"]
    groups_helper.delete_alarm(request, group_name, alarm_id)
    return get_alarms(request, group_name)


def add_alarms(request, group_name):
    params = request.POST
    try:
        alarm_info = {}
        action_type = params["asgActionType"]
        if action_type == "grow":
            alarm_info["actionType"] = "GROW"
        else:
            alarm_info["actionType"] = "SHRINK"
        alarm_info["comparator"] = params["comparators"]
        alarm_info["threshold"] = float(params["threshold"])
        alarm_info["evaluationTime"] = int(params["evaluate_time"])
        if "customUrlCheckbox" in params:
            alarm_info["fromAwsMetric"] = False
            if "metricUrl" in params:
                alarm_info["metricSource"] = params["metricUrl"]
        else:
            alarm_info["fromAwsMetric"] = True
            if "awsMetrics" in params:
                alarm_info["metricSource"] = params["awsMetrics"]
        alarm_info["groupName"] = group_name
        groups_helper.add_alarm(request, group_name, [alarm_info])
    except:
        log.error(traceback.format_exc())

    return redirect("/groups/{}/config/".format(group_name))


# group host information
def get_group_info(request, group_name):
    try:
        group_info = groups_helper.get_group_info(request, group_name)
        launch_config = group_info.get("launchInfo")
        asgs = groups_helper.get_autoscaling(request, group_name)
        spot_asg = None
        nonspot_asg = None
        spot_asg_instances = []
        nonspot_asg_instances = []
        if asgs:
            for asg in asgs:
                if asg.get("spotGroup", None):
                    spot_asg = asg
                    spot_asg_instances = asg.get("instances")
                else:
                    nonspot_asg = asg
                    nonspot_asg_instances = asg.get("instances")

        all_hosts_in_group = groups_helper.get_group_instances(request, group_name)
        non_asg_host_names = []
        asg_host_names = []
        spot_asg_host_names = []
        if spot_asg:
            has_spot_group = True
        else:
            has_spot_group = False

        for host in all_hosts_in_group:
            host_name = host.get("hostName", "")
            host_id = host.get("hostId", "")
            if nonspot_asg_instances and host_id in nonspot_asg_instances:
                asg_host_names.append(host_name)
                nonspot_asg_instances.remove(host_id)
            elif spot_asg_instances and host_id in spot_asg_instances:
                spot_asg_host_names.append(host_name)
                spot_asg_instances.remove(host_id)
            else:
                non_asg_host_names.append(host_name)

        asg_host_names.extend(nonspot_asg_instances)
        spot_asg_host_names.extend(spot_asg_instances)

        if launch_config["asgStatus"] == "DISABLED":
            asg_status_str = "Disabled"
        elif launch_config["asgStatus"] == "ENABLED":
            asg_status_str = "Enabled"
        else:
            asg_status_str = "Not Enabled"
        group_size = len(asg_host_names) + len(non_asg_host_names) + len(spot_asg_host_names)
        spot_size = len(spot_asg_host_names)
        content = render_to_string("groups/group_info.tmpl", {
            "instance_type": launch_config["instanceType"],
            "security_group": launch_config["securityGroup"],
            "group_name": group_name,
            "fleet_size": group_size,
            "spot_size": spot_size,
            "asg": nonspot_asg,
            "spot_asg": spot_asg,
            "asg_status": asg_status_str,
            "asg_hosts": asg_host_names,
            "spot_asg_hosts": spot_asg_host_names,
            "other_hosts": non_asg_host_names,
            "has_spot_group": has_spot_group,
        })
        return HttpResponse(json.dumps({"html": content}), content_type="application/json")
    except:
        log.error(traceback.format_exc())


def get_group_size(request, group_name):
    try:
        group_size_datum = \
            autoscaling_metrics_helper.get_asg_size_metric(request, group_name,
                                                           settings.DEFAULT_START_TIME)

        spot_group_size_datum = \
            autoscaling_metrics_helper.get_asg_size_metric(request, "{}-spot".format(group_name),
                                                           settings.DEFAULT_START_TIME)

        alarm_infos = groups_helper.get_alarms(request, group_name)
        spot_group_name = "{}-spot".format(group_name)
        spot_alarm_infos = groups_helper.get_alarms(request, spot_group_name)
        enable_policy = False
        if alarm_infos and len(alarm_infos) > 0:
            enable_policy = True

        removeIdx = []
        if spot_alarm_infos:
            has_spot_asg = True
        else:
            has_spot_asg = False

        if alarm_infos:
            alarm_infos = sorted(alarm_infos, key=lambda info: info["metricSource"])
            for idx in xrange(len(alarm_infos)):
                alarm_info = alarm_infos[idx]
                alarm_infos[idx]["actionType2"] = "UNKNOWN"
                alarm_infos[idx]["threshold2"] = -1
                if spot_alarm_infos:
                    alarm_infos[idx]["spotActionType"] = spot_alarm_infos[idx]["actionType"]
                    alarm_infos[idx]["spotThreshold"] = spot_alarm_infos[idx]["threshold"]
                    alarm_infos[idx]["spotActionType2"] = "UNKNOWN"
                    alarm_infos[idx]["spotThreshold2"] = -1
                else:
                    alarm_infos[idx]["spotActionType"] = "UNKNOWN"
                    alarm_infos[idx]["spotThreshold"] = -1
                    alarm_infos[idx]["spotActionType2"] = "UNKNOWN"
                    alarm_infos[idx]["spotThreshold2"] = -1

                metric_name = alarm_info["metricSource"]
                if idx > 0 and metric_name == alarm_infos[idx - 1]["metricSource"]:
                    alarm_infos[idx - 1]["actionType2"] = alarm_info["actionType"]
                    alarm_infos[idx - 1]["threshold2"] = alarm_info["threshold"]
                    if spot_alarm_infos:
                        alarm_infos[idx - 1]["spotActionType2"] = spot_alarm_infos[idx]["actionType"]
                        alarm_infos[idx - 1]["spotThreshold2"] = spot_alarm_infos[idx]["threshold"]
                    removeIdx.append(idx)
                else:
                    alarm_info["metric_datum"] = \
                        autoscaling_metrics_helper.get_metric_data(request, group_name,
                                                                   metric_name,
                                                                   settings.DEFAULT_START_TIME)

        for offset, idx in enumerate(removeIdx):
            idx -= offset
            del alarm_infos[idx]

        content = render_to_string('groups/group_stats.tmpl', {
            "group_name": group_name,
            "enable_policy": enable_policy,
            "alarm_infos": alarm_infos,
            "has_spot_asg": has_spot_asg,
            "spot_alarm_infos": spot_alarm_infos,
            "group_size_datum": group_size_datum,
            "spot_group_size_datum": spot_group_size_datum,
        })

        return HttpResponse(json.dumps({"html": content}), content_type="application/json")
    except:
        log.error(traceback.format_exc())


def get_envs(request, group_name):
    envs = environs_helper.get_all_envs_by_group(request, group_name)
    html = render_to_string('groups/group_envs.tmpl', {
        "envs": envs,
    })
    return HttpResponse(html)


def get_scaling_activities(request, group_name):
    try:
        scaling_activities = groups_helper.get_scaling_activities(request, group_name,
                                                                  10, "")

        content = render_to_string("groups/scaling_details.tmpl", {
            "group_name": group_name,
            "activities": scaling_activities["activities"],
        })
        return HttpResponse(json.dumps({"html": content}), content_type="application/json")
    except:
        log.error(traceback.format_exc())


class ScalingActivityView(View):
    def get(self, request, group_name):

        scaling_activities_info = groups_helper.get_scaling_activities(request, group_name,
                                                                       50, "")
        activities = scaling_activities_info["activities"]
        next_token = scaling_activities_info["nextToken"]
        if next_token:
            disableNext = False
        else:
            disableNext = True
        return render(request, 'groups/scaling_activities.html', {
            "activities": activities,
            "next_token": next_token,
            "group_name": group_name,
            "disableNext": disableNext,
        })


def get_more_scaling_activities(request, group_name):
    params = request.GET
    token = params.get("token", "")
    try:
        scaling_activities_info = groups_helper.get_scaling_activities(request, group_name,
                                                                       50, token)
        activities = scaling_activities_info["activities"]
        next_token = scaling_activities_info["nextToken"]

        if next_token:
            disableNext = False
        else:
            disableNext = True

        content = render_to_string("groups/scaling_activities.tmpl", {
            "token": token,
            "group_name": group_name,
            "next_token": next_token,
            "activities": activities,
            "disableNext": disableNext,
        })
        return HttpResponse(json.dumps({"html": content}), content_type="application/json")
    except:
        log.error(traceback.format_exc())


def get_config_history(request, group_name):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    configs = groups_helper.get_config_history(request, group_name, index, size)
    for config in configs:
        replaced_config = config["configChange"].replace(",", ", ").replace("#", "%23").replace("\"", "%22")\
            .replace("{", "%7B").replace("}", "%7D").replace("_", "%5F")
        config["replaced_config"] = replaced_config

    return render(request, 'groups/group_config_history.html', {
        "group_name": group_name,
        "configs": configs,
        "pageIndex": index,
        "pageSize": DEFAULT_PAGE_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(configs) < DEFAULT_PAGE_SIZE,
    })


def _parse_config_comparison(query_dict):
    configs = {}
    for key, value in query_dict.iteritems():
        if key.startswith('chkbox_'):
            id = key[len('chkbox_'):]
            split_data = value.split('_')
            config_change = split_data[1]
            configs[id] = config_change
    return configs


def get_config_comparison(request, group_name):
    configs = _parse_config_comparison(request.POST)
    if len(configs) > 1:
        ids = configs.keys()
        change1 = configs[ids[0]]
        change2 = configs[ids[1]]

    return HttpResponse(json.dumps({'change1': change1, 'change2': change2}), content_type="application/json")


def show_config_comparison(request, group_name):
    change1 = request.GET.get('change1')
    change2 = request.GET.get('change2')
    diff_res = GenerateDiff()
    result = diff_res.diff_main(change1, change2)
    diff_res.diff_cleanupSemantic(result)
    old_change = diff_res.old_content(result)
    new_change = diff_res.new_content(result)

    return render(request, 'groups/group_config_comparison_result.html', {
        "group_name": group_name,
        "oldChange": old_change,
        "newChange": new_change,
    })


def config_rollback(request, group_name):
    changeId = request.GET.get('changeId')
    groups_helper.rollback_config(request, group_name, changeId)
    return redirect('/groups/{}/config/'.format(group_name))


def get_configs(request):
    params = request.GET
    groupName = params["group_name"]
    group_info = groups_helper.get_group_info(request, groupName)
    sorted_subnets = None
    sorted_sgs = None
    config = None
    if group_info:
        config = group_info.get("launchInfo")
        instance_types, sorted_subnets, sorted_sgs = get_system_specs(request)
        if config:
            config["subnetArrays"] = config["subnets"].split(',')
            config["userData"] = config["userData"].replace("\n", "<br>")

    contents = render_to_string('groups/get_config.tmpl', {
        "groupName": groupName,
        "subnets": sorted_subnets,
        "security_groups": sorted_sgs,
        "config": config,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


class GenerateDiff(diff_match_patch):
    def old_content(self, diffs):
        html = []
        for (flag, data) in diffs:
            text = (data.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>")
                    .replace(",", ",<br>"))

            if flag == self.DIFF_DELETE:
                html.append("""<b style=\"background:#FFB5B5;
                    \">%s</b>""" % text)
            elif flag == self.DIFF_EQUAL:
                html.append("<span>%s</span>" % text)
        return "".join(html)

    def new_content(self, diffs):
        html = []
        for (flag, data) in diffs:
            text = (data.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>")
                    .replace(",", ",<br>"))
            if flag == self.DIFF_INSERT:
                html.append("""<b style=\"background:#97f697;
                    \">%s</b>""" % text)
            elif flag == self.DIFF_EQUAL:
                html.append("<span>%s</span>" % text)
        return "".join(html)


class GroupConfigView(View):
    def get(self, request, group_name):
        asg_cluster = groups_helper.get_group_info(request, group_name)
        appNames = images_helper.get_all_app_names(request)
        appNames = sorted(appNames)
        is_cmp = False
        if asg_cluster:
            asg_vm_info = asg_cluster.get("launchInfo")
            if asg_vm_info and asg_vm_info.get("subnets"):
                 asg_vm_info["subnetArrays"] = asg_vm_info["subnets"].split(',')
            group_info = asg_cluster.get("groupInfo")
            curr_image = images_helper.get_image_by_id(request, asg_vm_info["imageId"])
            group_info = get_group_config_internal(group_info)
            envs = environs_helper.get_all_envs_by_group(request, group_name)
            for env in envs:
                basic_cluster_info = clusters_helper.get_cluster(request, env.get('envName'), env.get('stageName'))
                if basic_cluster_info:
                    is_cmp = True
        else:
            asg_vm_info = None
            group_info = None
            curr_image = None

        pas_config = groups_helper.get_pas_config(request, group_name)

        return render(request, 'groups/asg_config.html', {
            "asg_vm_config": asg_vm_info,
            "app_names": appNames,
            "curr_image": curr_image,
            "group_config": group_info,
            "group_name": group_name,
            "pas_config": pas_config,
            "is_cmp": is_cmp
        })


class GroupDetailView(View):
    def get(self, request, group_name):
        autoscaling_summary = groups_helper.get_autoscaling_summary(request, group_name)
        asg_status = autoscaling_summary.get("status", "UNKNOWN")
        enable_spot = autoscaling_summary.get("enableSpot", False)
        envs = environs_helper.get_all_envs_by_group(request, group_name)
        scaling_down_event_enabled = groups_helper.get_scaling_down_event_status(request, group_name)
        return render(request, 'groups/group_details.html', {
            "asg_status": asg_status,
            "enable_spot": enable_spot,
            "group_name": group_name,
            "scaling_down_event_enabled": scaling_down_event_enabled,
            "envs": envs,
        })


# generate aws related settings
def get_aws_settings(request):
    params = request.GET
    app_name = params["app_name"]
    images = images_helper.get_all_images_by_app(request, app_name)
    contents = render_to_string("groups/get_ami.tmpl",
                                {"aws_images": images,
                                 "curr_image_id": params["curr_image_id"]})
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_sg_settings(request):
    security_groups = specs_helper.get_security_groups(request)
    sorted_sgs = sorted(security_groups, key=lambda sg: sg["info"]["name"])
    curr_sg = request.GET.get("curr_sg", "")
    contents = render_to_string("groups/get_sg.tmpl",
                                {"security_groups": sorted_sgs,
                                 "curr_sg": curr_sg})
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_instance_type_settings(request):
    instance_types = specs_helper.get_instance_types(request)
    curr_instance_type = request.GET.get("curr_instance_type", "")
    contents = render_to_string("groups/get_hosttype.tmpl",
                                {"instance_types": instance_types,
                                 "curr_instance_type": curr_instance_type})
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_subnets_settings(request):
    subnets = specs_helper.get_subnets(request)
    sorted_subnets = sorted(subnets, key=lambda subnet: subnet["info"]["tag"])
    curr_subnets = request.GET.get("curr_id", "")
    if curr_subnets:
        currSubnetArrays = curr_subnets.split(',')
    else:
        currSubnetArrays = []
    contents = render_to_string("groups/get_subnets.tmpl",
                                {"subnets": sorted_subnets,
                                 "currSubnetArrays": currSubnetArrays})
    return HttpResponse(json.dumps(contents), content_type="application/json")


# Instances Provision
def add_instance(request, group_name):
    params = request.POST
    num = int(params["instanceCnt"])
    subnet = None
    asg_status = params['asgStatus']
    launch_in_asg = True
    if 'subnet' in params:
        subnet = params['subnet']
        if asg_status == 'UNKNOWN':
            launch_in_asg = False
        elif 'customSubnet' in params:
            launch_in_asg = False
    try:
        if not launch_in_asg:
            host_ids = groups_helper.launch_instance_in_group(request, group_name, num, subnet)
            if len(host_ids) > 0:
                content = '{} hosts have been launched to group {} (host ids: {})'.format(num, group_name, host_ids)
                messages.add_message(request, messages.SUCCESS, content)
            else:
                content = 'Failed to launch hosts to group {}. Please make sure the' \
                          ' <a href="https://deploy.pinadmin.com/groups/{}/config/">group config</a>' \
                          ' is correct. If you have any question, please contact your friendly Teletraan owners' \
                          ' for immediate assistance!'.format(group_name, group_name)
                messages.add_message(request, messages.ERROR, content)
        else:
            groups_helper.launch_instance_in_group(request, group_name, num, None)
            content = 'Capacity increased by {} for Auto Scaling Group {}. Please go to ' \
                      '<a href="https://deploy.pinadmin.com/groups/{}/">group page</a> ' \
                      'to check new hosts information.'.format(num, group_name, group_name)
            messages.add_message(request, messages.SUCCESS, content)
    except:
        log.error(traceback.format_exc())
        raise
    return redirect('/groups/{}'.format(group_name))


def instance_action_in_asg(request, group_name):
    host_id = request.GET.get("hostId", "")
    action = request.GET.get("action", "")
    host_ids = []
    host_ids.append(host_id)
    try:
        groups_helper.instance_action_in_group(request, group_name, host_ids, action)
    except:
        log.error(traceback.format_exc())
        raise
    return redirect('/groups/{}'.format(group_name))


# Health Check related
def get_health_check_activities(request, group_name):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    health_checks = groups_helper.get_health_check_activities(request, group_name, index, size)
    asg_status = groups_helper.get_autoscaling_status(request, group_name)
    scaling_down_event_enabled = groups_helper.get_scaling_down_event_status(request, group_name)

    for check in health_checks:
        env_id = check.get('env_id')
        env = environs_helper.get(request, env_id)
        check['env_name'] = env.get('envName')
        check['stage_name'] = env.get('stageName')

    return render(request, 'groups/health_check_activities.html', {
        "group_name": group_name,
        "health_checks": health_checks,
        "asg_status": asg_status,
        "scaling_down_event_enabled": scaling_down_event_enabled,
        "pageIndex": index,
        "pageSize": DEFAULT_PAGE_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(health_checks) < DEFAULT_PAGE_SIZE
    })


def get_health_check_details(request, id):
    health_check = groups_helper.get_health_check(request, id)
    env = environs_helper.get(request, health_check.get('env_id'))
    health_check['env_name'] = env.get('envName')
    health_check['stage_name'] = env.get('stageName')

    health_check_error = groups_helper.get_health_check_error(request, id)
    if health_check_error:
        env = environs_helper.get(request, health_check.get('env_id'))
        health_check_error['env_name'] = env.get('envName')
        health_check_error['stage_name'] = env.get('stageName')

    return render(request, 'groups/health_check_details.html', {
        "health_check": health_check,
        "health_check_error": health_check_error
    })


def create_manually_health_check(request, group_name):
    health_check_info = {}
    health_check_info["group_name"] = group_name
    health_check_info["type"] = "MANUALLY_TRIGGERED"
    groups_helper.create_health_check(request, group_name, health_check_info)
    return redirect('/groups/{}/health_check_activities'.format(group_name))


def enable_scaling_down_event(request, group_name):
    groups_helper.enable_scaling_down_event(request, group_name)
    return redirect('/groups/{}'.format(group_name))


def disable_scaling_down_event(request, group_name):
    groups_helper.disable_scaling_down_event(request, group_name)
    return redirect('/groups/{}'.format(group_name))


def add_scheduled_actions(request, group_name):
    params = request.POST
    try:
        schedule_action = {}
        schedule_action['clusterName'] = group_name
        schedule_action['schedule'] = params['schedule']
        schedule_action['capacity'] = params['capacity']
        groups_helper.add_scheduled_actions(request, group_name, [schedule_action])
    except:
        log.error(traceback.format_exc())
    return redirect("/groups/{}/config/".format(group_name))


def get_scheduled_actions(request, group_name):
    scheduled_actions = groups_helper.get_scheduled_actions(request, group_name)
    content = render_to_string("groups/asg_schedules.tmpl", {
        'group_name': group_name,
        'scheduled_actions': scheduled_actions,
        'csrf_token': get_token(request),
    })
    return HttpResponse(json.dumps(content), content_type="application/json")


def delete_scheduled_actions(request, group_name):
    params = request.POST
    action_id = params['action_id']
    groups_helper.delete_scheduled_action(request, group_name, action_id)
    return get_scheduled_actions(request, group_name)


def _parse_actions_configs(query_data, group_name):
    page_data = dict(query_data.lists())
    configs = []
    for key, value in page_data.iteritems():
        if not value:
            continue
        if key.startswith('TELETRAAN_'):
            action_info = {}
            action_id = key[len('TELETRAAN_'):]
            action_info["actionId"] = action_id
            action_info["schedule"] = page_data["schedule_{}".format(action_id)][0]
            action_info["capacity"] = page_data["capacity_{}".format(action_id)][0]
            action_info["clusterName"] = group_name
            configs.append(action_info)
    return configs


def update_scheduled_actions(request, group_name):
    try:
        configs = _parse_actions_configs(request.POST, group_name)
        groups_helper.add_scheduled_actions(request, group_name, configs)
        return get_scheduled_actions(request, group_name)
    except:
        log.error(traceback.format_exc())
        return HttpResponse(json.dumps({'content': ""}), content_type="application/json")
