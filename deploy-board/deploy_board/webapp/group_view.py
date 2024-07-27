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


from deploy_board.settings import IS_PINTEREST, PHOBOS_URL
from django.middleware.csrf import get_token
from django.shortcuts import render, redirect
from django.views.generic import View
from django.template.loader import render_to_string
from django.http import HttpResponse
from django.contrib import messages
from django.contrib.messages import get_messages
from collections import Counter
import json
import logging
import traceback

from .helpers import (environs_helper, clusters_helper, hosttypes_helper, groups_helper, baseimages_helper,
                     specs_helper, autoscaling_groups_helper, autoscaling_metrics_helper, placements_helper,
                     hosts_helper, accounts_helper)
from diff_match_patch import diff_match_patch
from deploy_board import settings
from .helpers.exceptions import NotFoundException, TeletraanException

log = logging.getLogger(__name__)


DEFAULT_PAGE_SIZE = 50

ScalingType = ["ChangeInCapacity", "ExactCapacity", "PercentChangeInCapacity"]


def group_landing(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    group_names = autoscaling_groups_helper.get_env_group_names(request, index, size)
    return render(request, 'groups/group_landing.html', {
        'group_names': group_names,
        "pageIndex": index,
        "pageSize": DEFAULT_PAGE_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(group_names) < DEFAULT_PAGE_SIZE,
    })


def get_group_names(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    group_names = autoscaling_groups_helper.get_env_group_names(request, index, size)
    return HttpResponse(json.dumps(group_names), content_type="application/json")


def search_groups(request, group_name):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    group_names = autoscaling_groups_helper.get_env_group_names(request, index, size, name_filter=group_name)

    if not group_names:
        return redirect('/groups/')

    if len(group_names) == 1:
        return redirect('/groups/%s/' % group_names[0])

    return render(request, 'groups/group_landing.html', {
    "group_names": group_names,
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
        group_info = autoscaling_groups_helper.get_group_info(request, group_name)
        launch_config = group_info.get("launchInfo")

        if launch_config and launch_config.get("subnets"):
            launch_config["subnetArrays"] = launch_config["subnets"].split(',')
        appNames = baseimages_helper.get_image_names(request, 'AWS', settings.DEFAULT_CELL)
        appNames = sorted(appNames)
        curr_image = baseimages_helper.get_by_provider_name(request, launch_config["imageId"])
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
            group_config["launchLatencyTh"] = group_config.get("launchLatencyTh") // 60
        if group_config.get("healthcheckPeriod"):
            group_config["healthcheckPeriod"] = group_config.get("healthcheckPeriod") // 60
        if group_config.get("lifecycleTimeout"):
            group_config["lifecycleTimeout"] = group_config.get("lifecycleTimeout") // 60
        return group_config
    else:
        group_config = {}
        group_config["launchLatencyTh"] = 10
        group_config["healthcheckPeriod"] = 10
        group_config["lifecycleTimeout"] = 10
        return group_config


def get_group_config(request, group_name):
    try:
        group_info = autoscaling_groups_helper.get_group_info(request, group_name)
        group_config = group_info.get("groupInfo")
        group_config = get_group_config_internal(group_config)
        is_cmp = False
        envs = environs_helper.get_all_envs_by_group(request, group_name)
        for env in envs:
            basic_cluster_info = clusters_helper.get_cluster(request, env.get('clusterName'))
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
        if not params['metric'] or not params['throughput']:
            raise Exception("All fields for Predictive Autoscaling Config Must be specified. ")
        data = {}
        data['group_name'] = group_name
        data["metric"] = params["metric"]
        data["throughput"] = int(params["throughput"])
        if "pas_state" in params:
            data["pas_state"] = "ENABLED"
        else:
            data["pas_state"] = "DISABLED"
            # Reset min size to user defined min size
            pas_config = autoscaling_groups_helper.get_pas_config(request, group_name)
            asg_request = {}
            asg_request["groupName"] = group_name
            asg_request["minSize"] = int(pas_config.get("defined_min_size"))
            autoscaling_groups_helper.update_autoscaling(request, group_name, asg_request)
        autoscaling_groups_helper.update_pas_config(request, group_name, data)
        return get_pas_config(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise


def get_pas_config(request, group_name):
    try:
        pas_config = autoscaling_groups_helper.get_pas_config(request, group_name)
        html = render_to_string('groups/pase_config.tmpl', {
            "group_name": group_name,
            "pas_config": pas_config,
            "csrf_token": get_token(request),
        })
    except:
        log.error(traceback.format_exc())
        raise
    return HttpResponse(json.dumps(html), content_type="application/json")

def get_spiffe_id(user_data):
    for entry in user_data:
        tokens = entry.split(":", 1)
        if tokens[0].strip() == "spiffe_id":
            return tokens[1].strip()
    return None

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

        desired_spiffe_id = get_spiffe_id(params["userData"].splitlines())

        actual_spiffe_id = None
        group_info = autoscaling_groups_helper.get_group_info(request, group_name)
        launch_config = group_info.get("launchInfo")
        if 'userData' in launch_config:
            actual_spiffe_id = get_spiffe_id(launch_config['userData'].splitlines())

        if desired_spiffe_id != actual_spiffe_id:
            log.error("Teletraan does not allow spiffe_id changes from user")
            raise TeletraanException("Teletraan does not allow spiffe_id changes from user")

        autoscaling_groups_helper.update_launch_config(request, group_name, launchRequest)
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
        autoscaling_groups_helper.create_launch_config(request, group_name, launchRequest)
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
        groupRequest["loadBalancers"] = params.get("load_balancers")
        groupRequest["targetGroups"] = params.get("target_groups")
        groupRequest["runbookLink"] = params.get("runbook_link")

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
        if "lifecycle_notifications" in params:
            groupRequest["lifecycleNotifications"] = True
        else:
            groupRequest["lifecycleNotifications"] = False

        autoscaling_groups_helper.update_group_info(request, group_name, groupRequest)
        return get_group_config(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise


def gen_asg_setting(request, group_name):
    asg = autoscaling_groups_helper.get_autoscaling(request, group_name)
    policies = autoscaling_groups_helper.TerminationPolicy
    content = render_to_string("groups/create_asg_modal.tmpl", {
        "asg": asg,
        "group_name": group_name,
        "policies": policies,
        "csrf_token": get_token(request)})
    return HttpResponse(content)


def disable_asg(request, group_name):
    autoscaling_groups_helper.disable_autoscaling(request, group_name)
    return redirect('/groups/{}/'.format(group_name))


def resume_asg(request, group_name):
    autoscaling_groups_helper.enable_autoscaling(request, group_name)
    return redirect('/groups/{}/'.format(group_name))


def create_asg(request, group_name):
    params = request.POST
    asgRequest = {}
    asgRequest["minSize"] = int(params["min_size"])
    asgRequest["maxSize"] = int(params["max_size"])
    asgRequest["terminationPolicy"] = params["terminationPolicy"]

    autoscaling_groups_helper.create_autoscaling(request, group_name, asgRequest)
    return redirect('/groups/{}/config/'.format(group_name))


def get_asg_config(request, group_name):
    asg_summary = autoscaling_groups_helper.get_autoscaling_summary(request, group_name)
    pas_config = autoscaling_groups_helper.get_pas_config(request, group_name)
    instances = groups_helper.get_group_hosts(request, group_name)
    group_info = autoscaling_groups_helper.get_group_info(request, group_name)
    launch_config = group_info.get("launchInfo")
    group_size = len(instances)
    policies = autoscaling_groups_helper.TerminationPolicy
    if asg_summary.get("sensitivityRatio", None):
        asg_summary["sensitivityRatio"] *= 100

    placements = None
    try:
        basic_cluster_info = clusters_helper.get_cluster(request, group_name)
        if basic_cluster_info:
            account_id = basic_cluster_info.get("accountId")
            placements = placements_helper.get_simplified_by_ids(
                request, account_id, basic_cluster_info['placement'],
                basic_cluster_info['provider'], basic_cluster_info['cellName'])
    except Exception as e:
        log.warning('Failed to get placements: {}'.format(e))
    content = render_to_string("groups/asg_config.tmpl", {
        "group_name": group_name,
        "asg": asg_summary,
        "group_size": group_size,
        "terminationPolicies": policies,
        "instanceType": launch_config.get("instanceType"),
        "csrf_token": get_token(request),
        "pas_config": pas_config,
        "placements": json.dumps(placements),
    })
    return HttpResponse(json.dumps(content), content_type="application/json")


def delete_asg(request, group_name):
    try:
        params = request.GET
        if params.get("delete") == "1":
            detach_instances = "false"
        else:
            detach_instances = "true"
        autoscaling_groups_helper.delete_autoscaling(request, group_name, detach_instances)
        content = render_to_string("groups/deletion_loading.tmpl",
                                   {"group_name": group_name})
    except:
        raise
    return HttpResponse(json.dumps(content), content_type="application/json")


def get_deleted_asg_status(request, group_name):
    try:
        status = autoscaling_groups_helper.get_autoscaling_status(request, group_name)
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

    # validate input params
    asg_minsize = int(params.get("minSize", -1))
    asg_maxsize = int(params.get("maxSize", -1))
    if asg_minsize < 0 or asg_maxsize < 0:
        # make sure the input values are not empty. the negative value case is already checked by ASG, not an issue.
        raise TeletraanException("Please put valid values (>=0) in Auto Scaling Group config's Min Size and Max Size fields")

    try:
        asg_request = {}
        asg_request["groupName"] = group_name
        asg_request["minSize"] = asg_minsize
        asg_request["maxSize"] = asg_maxsize
        asg_request["terminationPolicy"] = params["terminationPolicy"]
        if "enableSpot" in params:
            asg_request["enableSpot"] = True
            asg_request["spotMinSize"] = int(params["spotMinSize"])
            asg_request["spotMaxSize"] = int(params["spotMaxSize"])
            asg_request["sensitivityRatio"] = float(params["sensitivityRatio"]) / 100
            asg_request["spotPrice"] = params["bidPrice"]
            if "enableResourceLending" in params:
                asg_request["enableResourceLending"] = True
            else:
                asg_request["enableResourceLending"] = False
        else:
            asg_request["enableSpot"] = False
            asg_request["enableResourceLending"] = False

        # for time based asg, validate the capacity in scheduled actions against asg's min and max size
        scheduled_actions = autoscaling_groups_helper.get_scheduled_actions(request, group_name)
        if len(scheduled_actions) > 0:
            for action in scheduled_actions:
                capacity = action.get("capacity")
                if capacity is not None and (capacity < asg_minsize or capacity > asg_maxsize):
                    raise TeletraanException("Desired capacity in the scheduled scaling actions is invalid. Please update the scheduled actions to make sure all the desired capacities are within the limits of ASG's minimum capacity ({}) and maximum capacity ({})".format(asg_minsize, asg_maxsize))

        autoscaling_groups_helper.update_autoscaling(request, group_name, asg_request)

        # Save new pas min and max, disable pas
        pas_config = {}
        pas_config['group_name'] = group_name
        pas_config['defined_min_size'] = asg_minsize
        pas_config['defined_max_size'] = asg_maxsize
        pas_config['pas_state'] = "DISABLED"
        autoscaling_groups_helper.update_pas_config(request, group_name, pas_config)
    except:
        log.error(traceback.format_exc())
        raise

    return get_asg_config(request, group_name)


def make_int(s):
    s = s.strip()
    return int(s) if s else 0


def get_policy(request, group_name):
    policies = autoscaling_groups_helper.get_policies(request, group_name)
    step_scaling_policy = None
    for policy in policies["scalingPolicies"]:
        if policy["policyType"] == "StepScaling" and step_scaling_policy is None:
            step_scaling_policy = policy
        if policy["policyType"] in ["TargetTrackingScaling", "StepScaling"]:
            if policy["instanceWarmup"]:
                policy["instanceWarmup"] = policy["instanceWarmup"] // 60
            else:
                policy["instanceWarmup"] = 0

    scale_up_steps = []
    scale_down_steps = []

    if step_scaling_policy is not None:
        for step in step_scaling_policy["stepAdjustments"]:
            if step["metricIntervalUpperBound"] is not None and float(step["metricIntervalUpperBound"]) <= 0:
                scale_down_steps.append({"upper_bound": float(step["metricIntervalUpperBound"]), "adjustment": step["scalingAdjustment"]})
            if step["metricIntervalLowerBound"] is not None and float(step["metricIntervalLowerBound"]) >= 0:
                scale_up_steps.append({"lower_bound": float(step["metricIntervalLowerBound"]), "adjustment": step["scalingAdjustment"]})

        scale_down_steps = sorted(scale_down_steps, key=lambda d: d['upper_bound'])
        scale_up_steps = sorted(scale_up_steps, key=lambda d: d['lower_bound'])

        scale_down_steps_string = ", ".join([str(step['upper_bound']) for step in scale_down_steps])
        scale_up_steps_string = ", ".join([str(step['lower_bound']) for step in scale_up_steps])

        scale_down_adjustments_string = ", ".join([str(step['adjustment']) for step in scale_down_steps])
        scale_up_adjustments_string = ", ".join([str(step['adjustment']) for step in scale_up_steps])

        step_scaling_policy["scale_down_steps_string"] = scale_down_steps_string
        step_scaling_policy["scale_up_steps_string"] = scale_up_steps_string
        step_scaling_policy["scale_down_adjustments_string"] = scale_down_adjustments_string
        step_scaling_policy["scale_up_adjustments_string"] = scale_up_adjustments_string

    content = render_to_string("groups/asg_policy.tmpl", {
        "group_name": group_name,
        "scalingPolicies": policies["scalingPolicies"],
        "scaleupPolicies": policies["scaleupPolicies"],
        "scaledownPolicies": policies["scaledownPolicies"],
        "stepScalingPolicy": step_scaling_policy,
        "csrf_token": get_token(request),
    })

    return HttpResponse(json.dumps(content), content_type="application/json")

def build_target_scaling_policy(name, metric, target, instance_warmup, disable_scale_in):
    policy = {}
    policy["policyType"] = "TargetTrackingScaling"
    policy["policyName"] = name
    policy["instanceWarmup"] = int(instance_warmup) * 60

    targetTrackingScalingConfiguration = {}
    targetTrackingScalingConfiguration["targetValue"] = target
    targetTrackingScalingConfiguration["disableScaleIn"] = disable_scale_in
    predefinedMetricSpecification = {}
    predefinedMetricSpecification["predefinedMetricType"] = metric
    targetTrackingScalingConfiguration["predefinedMetricSpecification"] = predefinedMetricSpecification

    policy["targetTrackingScalingConfiguration"] = targetTrackingScalingConfiguration

    return policy

def delete_policy(request, group_name, policy_name):
    try:
        autoscaling_groups_helper.delete_scaling_policy(request, group_name, policy_name)
        return get_policy(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise

def update_policy(request, group_name):

    try:
        params = request.POST
        policyType = params["policyType"]
        scaling_policies = {}
        isNewPolicyAdded = False

        if policyType == "target-tracking-scaling":
            scaling_policies["scalingPolicies"] = []

            if "content" in params:
                # update existing policies
                content = params["content"]
                input = dict(kv.split("=", 1) for kv in content.split("&"))
                policy_names = {key: val for key, val in input.items()
                    if key.startswith("targetScalingName_")}

                for name in policy_names.values():
                    disable_scale_in = False
                    if name + "_disableScaleIn" in input:
                        disable_scale_in = True
                    policy = build_target_scaling_policy(name, input[name+"_awsMetrics"], input[name+"_target"], input[name+"_instanceWarmup"], disable_scale_in)
                    scaling_policies["scalingPolicies"].append(policy)
            else:
                # Create new policy
                isNewPolicyAdded = True
                metric = params["awsMetrics"]
                name = "{}-TargetTrackingScaling-{}".format(group_name, metric)
                target = params["target"]
                instance_warmup = params["instanceWarmup"]
                disable_scale_in = False
                if "disableScaleIn" in params:
                    disable_scale_in = True

                policy = build_target_scaling_policy(name, metric, target, instance_warmup, disable_scale_in)
                scaling_policies["scalingPolicies"].append(policy)
        elif policyType == "simple-scaling":
            scaling_policies["scaleupPolicies"] = []
            scaling_policies["scaledownPolicies"] = []
            scaling_policies["scaleupPolicies"].append({"scaleSize": make_int(params["scaleupSize"]),
                                                        "scalingType": params["scaleupType"],
                                                        "coolDown": make_int(params["scaleupCooldownTime"])})
            scaling_policies["scaledownPolicies"].append({"scaleSize": make_int(params["scaledownSize"]),
                                                        "scalingType": params["scaledownType"],
                                                        "coolDown": make_int(params["scaleDownCooldownTime"])})
        elif policyType == "step-scaling":
            scaling_policies["scalingPolicies"] = []
            step_scaling_policy = {}
            step_scaling_policy["policyType"] = "StepScaling"
            step_scaling_policy["scalingType"] = params["scalingType"]

            if params["instanceWarmup"]:
                step_scaling_policy["instanceWarmup"] = int(params["instanceWarmup"]) * 60

            step_scaling_policy["metricAggregationType"] = "Average"

            if params["scalingType"] == "PercentChangeInCapacity":
                step_scaling_policy["minAdjustmentMagnitude"] = params["minAdjustmentMagnitude"]

            step_scaling_policy["stepAdjustments"] = []

            if (params['scaleUpSteps']):
                scaleUpSteps = [float(x) for x in params["scaleUpSteps"].split(',')]
                scaleUpAdjustments = [int(x) for x in params["scaleUpAdjustments"].split(',')]
                for i in range(len(scaleUpSteps)):
                    step = {}
                    step["metricIntervalLowerBound"] = scaleUpSteps[i]
                    if i < len(scaleUpSteps) - 1:
                        step["metricIntervalUpperBound"] = scaleUpSteps[i + 1]
                    step["scalingAdjustment"] = scaleUpAdjustments[i]
                    step_scaling_policy["stepAdjustments"].append(step)

            if (params['scaleDownSteps']):
                scaleDownSteps = [float(x) for x in params["scaleDownSteps"].split(',')]
                scaleDownAdjustments = [int(x) for x in params["scaleDownAdjustments"].split(',')]

                for i in range(len(scaleDownSteps)):
                    step = {}
                    step["metricIntervalUpperBound"] = scaleDownSteps[i]
                    if i > 0:
                        step["metricIntervalLowerBound"] = scaleDownSteps[i - 1]
                    step["scalingAdjustment"] = scaleDownAdjustments[i]
                    step_scaling_policy["stepAdjustments"].append(step)

            scaling_policies["scalingPolicies"].append(step_scaling_policy)

        autoscaling_groups_helper.put_scaling_policies(request, group_name, scaling_policies)

        if isNewPolicyAdded:
            return redirect("/groups/{}/config/".format(group_name))
        else:
            return get_policy(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise


# alarm related information
def _parse_metrics_configs(request, group_name):
    params = request.POST
    page_data = dict(params.lists())
    configs = []
    for key, value in page_data.items():
        if not value:
            continue
        if key.startswith('TELETRAAN_'):
            alarm_info = {}
            alarm_info["scalingPolicies"] = []

            alarm_id = key[len('TELETRAAN_'):]
            action_type = params["actionType_{}".format(alarm_id)]

            if page_data["fromAwsMetric_{}".format(alarm_id)][0] == "True":
                alarm_info["fromAwsMetric"] = True
            else:
                alarm_info["fromAwsMetric"] = False

            # skip scheduled actions
            if page_data.get("schedule_{}".format(alarm_id)) or page_data.get("capacity_{}".format(alarm_id)):
                continue

            policy_type = "simple-scaling"
            policy_type_key = "policyType_{}".format(alarm_id)
            if policy_type_key in page_data:
                policy_type = page_data[policy_type_key][0]

            policies = autoscaling_groups_helper.get_policies(request, group_name)

            # Use simple scaling for custom metric
            if page_data["fromAwsMetric_{}".format(alarm_id)][0] is False:
                policy_type = "simple-scaling"

            if policy_type == "simple-scaling":
                if action_type == "GROW":
                    if policies["scaleupPolicies"] is not None:
                        for i in policies["scaleupPolicies"]:
                            alarm_info["scalingPolicies"].append(i)
                else:
                    if policies["scaledownPolicies"] is not None:
                        for i in policies["scaledownPolicies"]:
                            alarm_info["scalingPolicies"].append(i)
            else:
                if policies["scalingPolicies"] is not None:
                    for p in policies["scalingPolicies"]:
                        if p["policyType"] ==  "StepScaling":
                            alarm_info["scalingPolicies"].append(p)
                            break

            alarm_info["alarmId"] = alarm_id
            alarm_info["actionType"] = page_data["actionType_{}".format(alarm_id)][0]
            alarm_info["metricSource"] = page_data["metricsUrl_{}".format(alarm_id)][0]
            alarm_info["comparator"] = page_data["comparator_{}".format(alarm_id)][0]
            alarm_info["evaluationTime"] = int(page_data["evaluateTime_{}".format(alarm_id)][0])
            alarm_info["threshold"] = float(page_data["threshold_{}".format(alarm_id)][0])
            alarm_info["groupName"] = group_name

            configs.append(alarm_info)

    return configs


def get_alarms(request, group_name):
    comparators = autoscaling_groups_helper.Comparator
    alarms = autoscaling_groups_helper.get_alarms(request, group_name)

    for alarm in alarms:
        alarm["scalingType"] = None
        if alarm["scalingPolicies"] is not None:
            if len(alarm["scalingPolicies"]) > 0:
                # we restrict alarm has only one scaling policy
                policy = alarm["scalingPolicies"][0]
                alarm["scalingType"] = policy["policyType"]

    aws_metric_names = [
        "CPUUtilization",
        "DiskReadOps",
        "DiskWriteOps",
        "DiskReadBytes",
        "DiskWriteBytes",
        "MetadataNoToken",
        "NetworkIn",
        "NetworkOut",
        "NetworkPacketsIn",
        "NetworkPacketsOut",
        "CPUCreditUsage",
        "CPUCreditBalance",
        "CPUSurplusCreditBalance",
        "CPUSurplusCreditsCharged",
        "DedicatedHostCPUUtilization",
        "EBSReadOps",
        "EBSWriteOps",
        "EBSReadBytes",
        "EBSWriteBytes",
        "EBSIOBalance%",
        "EBSByteBalance%",
        "StatusCheckFailed",
        "StatusCheckFailed_Instance",
        "StatusCheckFailed_System",
        "StatusCheckFailed_AttachedEBS"
    ]
    content = render_to_string("groups/asg_metrics.tmpl", {
        "group_name": group_name,
        "alarms": alarms,
        "aws_metric_names": json.dumps(aws_metric_names),
        "csrf_token": get_token(request),
        "comparators": json.dumps(comparators),
    })
    return HttpResponse(content)


def update_alarms(request, group_name):
    try:
        configs = _parse_metrics_configs(request, group_name)
        autoscaling_groups_helper.update_alarms(request, group_name, configs)
        return get_alarms(request, group_name)
    except Exception as ex:
        log.exception(ex)
        return HttpResponse(json.dumps({'success': False, 'error': str(ex)}), content_type="application/json")


def delete_alarms(request, group_name):
    params = request.POST
    alarm_id = params["alarmId"]
    autoscaling_groups_helper.delete_alarm(request, group_name, alarm_id)
    return get_alarms(request, group_name)


def add_alarms(request, group_name):
    params = request.POST
    alarm_info = {}
    alarm_info["scalingPolicies"] = []
    action_type = params["asgActionType"]
    policy_type = "simple-scaling"
    if "policyType" in params:
        policy_type = params["policyType"]

    policies = autoscaling_groups_helper.get_policies(request, group_name)

    # Use simple scaling for custom metric
    if "customUrlCheckbox" in params:
        policy_type = "simple-scaling"

    if policy_type == "simple-scaling":
        if action_type == "grow":
            if policies["scaleupPolicies"] is not None:
                for i in policies["scaleupPolicies"]:
                    alarm_info["scalingPolicies"].append(i)
        else:
            if policies["scaledownPolicies"] is not None:
                for i in policies["scaledownPolicies"]:
                    alarm_info["scalingPolicies"].append(i)
    else:
        if policies["scalingPolicies"] is not None:
            for p in policies["scalingPolicies"]:
                if p["policyType"] ==  "StepScaling":
                    alarm_info["scalingPolicies"].append(p)
                    break

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

    if len(alarm_info["scalingPolicies"]) == 0:
        # Technically, an alarm can be created without an action (e.g. scaling policy)
        # However, in the current context, an alarm must have an associated scaling policy.
        # In this case, there is no scaling policy, so refuse to create new alarm.
        messages.add_message(request, messages.ERROR, 'Alarm could not be created because there was no {} policy.'.format(policy_type))
        return redirect("/groups/{}/config/".format(group_name))

    autoscaling_groups_helper.add_alarm(request, group_name, [alarm_info])

    return redirect("/groups/{}/config/".format(group_name))


# group host information
def get_group_info(request, group_name):
    try:
        group_info = autoscaling_groups_helper.get_group_info(request, group_name)
        launch_config = group_info.get("launchInfo")
        asgs = autoscaling_groups_helper.get_autoscaling(request, group_name)
        spot_asg = None
        nonspot_asg = None
        spot_asg_instances = []
        nonspot_asg_instances = []
        if asgs:
            for asg in asgs:
                if asg.get("enableSpot", None):
                    spot_asg = asg
                    spot_asg_instances = asg.get("instances")
                else:
                    nonspot_asg = asg
                    nonspot_asg_instances = asg.get("instances")

        all_hosts_in_group = groups_helper.get_group_hosts(request, group_name)
        non_asg_host_names = []
        non_asg_host_ids = []
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
                non_asg_host_ids.append(host_id)

        non_asg_host_ids_str = ",".join(non_asg_host_ids)
        asg_host_names.extend(nonspot_asg_instances)
        spot_asg_host_names.extend(spot_asg_instances)

        if launch_config["asgStatus"] == "DISABLED":
            asg_status_str = "Disabled"
        elif launch_config["asgStatus"] == "ENABLED":
            asg_status_str = "Enabled"
        else:
            asg_status_str = "Not Enabled"
        asg_state = autoscaling_groups_helper.get_alarm_state(request, group_name)
        if asg_state == "UNKNOWN":
            asg_state_str = ""
        elif asg_state == "SCALE_UP_ALARM":
            asg_state_str = "Scaling up"
        elif asg_state == "SCALE_DOWN_ALARM":
            asg_state_str = "Scaling down"
        else:
            asg_state_str = "OK"

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
            "other_host_ids": non_asg_host_ids_str,
            "has_spot_group": has_spot_group,
            "asg_state": asg_state_str,
            "csrf_token": get_token(request),
        })
        return HttpResponse(json.dumps({"html": content}), content_type="application/json")
    except Exception:
        log.error(traceback.format_exc())


def get_group_size(request, group_name):
    try:
        group_size_datum = \
            autoscaling_metrics_helper.get_asg_size_metric(request, group_name,
                                                           settings.DEFAULT_START_TIME)

        spot_group_size_datum = \
            autoscaling_metrics_helper.get_asg_size_metric(request, "{}-spot".format(group_name),
                                                           settings.DEFAULT_START_TIME)

        alarm_infos = autoscaling_groups_helper.get_alarms(request, group_name)
        spot_group_name = "{}-spot".format(group_name)
        spot_alarm_infos = autoscaling_groups_helper.get_alarms(request, spot_group_name)
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
            for idx in range(len(alarm_infos)):
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
    except Exception:
        log.error(traceback.format_exc())


def get_envs(request, group_name):
    envs = environs_helper.get_all_envs_by_group(request, group_name)
    html = render_to_string('groups/group_envs.tmpl', {
        "envs": envs,
    })
    return HttpResponse(html)


def get_scaling_activities(request, group_name):
    try:
        scaling_activities = autoscaling_groups_helper.get_scaling_activities(request, group_name, 10, "")
        content = render_to_string("groups/scaling_details.tmpl", {
            "group_name": group_name,
            "activities": scaling_activities["activities"],
        })
        return HttpResponse(json.dumps({"html": content}), content_type="application/json")
    except Exception:
        log.error(traceback.format_exc())


class ScalingActivityView(View):
    def get(self, request, group_name):
        scaling_activities_info = autoscaling_groups_helper.get_scaling_activities(request, group_name, 50, "")
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
        scaling_activities_info = autoscaling_groups_helper.get_scaling_activities(request, group_name, 50, token)
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
    except Exception:
        log.error(traceback.format_exc())


def get_config_history(request, group_name):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    configs = autoscaling_groups_helper.get_config_history(request, group_name, index, size)
    for config in configs:
        replaced_config = config["configChange"].replace(",", ", ").replace("#", "%23").replace("\"", "%22")\
            .replace("{", "%7B").replace("}", "%7D").replace("_", "%5F")
        config["replaced_config"] = replaced_config
    excludedTypes = list(filter(None, request.GET.get("exclude", '').replace("%20", " ").split(",")))

    return render(request, 'groups/group_config_history.html', {
        "group_name": group_name,
        "configs": configs,
        "pageIndex": index,
        "pageSize": DEFAULT_PAGE_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(configs) < DEFAULT_PAGE_SIZE,
        "excludedTypes": excludedTypes
    })


def _parse_config_comparison(query_dict):
    configs = {}
    for key, value in query_dict.items():
        if key.startswith('chkbox_'):
            id = key[len('chkbox_'):]
            split_data = value.split('_')
            config_change = split_data[1]
            configs[id] = config_change
    return configs


def get_config_comparison(request, group_name):
    configs = _parse_config_comparison(request.POST)
    if len(configs) > 1:
        ids = list(configs.keys())
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


def get_configs(request):
    params = request.GET
    groupName = params["group_name"]
    group_info = autoscaling_groups_helper.get_group_info(request, groupName)
    sorted_subnets = None
    sorted_sgs = None
    config = None
    if group_info:
        config = group_info.get("launchInfo")
        instance_types, sorted_subnets, sorted_sgs = get_system_specs(request)
        if config:
            if config.get("subnets", None):
                config["subnetArrays"] = config["subnets"].split(',')
            else:
                config["subnetArrays"] = []
            config["userData"] = config["userData"].replace("\n", "<br>")

    contents = render_to_string('groups/get_config.tmpl', {
        "groupName": groupName,
        "subnets": sorted_subnets,
        "security_groups": sorted_sgs,
        "config": config,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def _disallow_autoscaling(curr_image):
    if IS_PINTEREST:
        # disallow autoscaling when the ami is using masterful puppet
        if curr_image and curr_image["abstract_name"] == "golden_12.04":
            return True
    return False

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
        asg_cluster = autoscaling_groups_helper.get_group_info(request, group_name)
        appNames = baseimages_helper.get_image_names(request, 'AWS', settings.DEFAULT_CELL)
        appNames = sorted(appNames)
        is_cmp = False
        if asg_cluster:
            asg_vm_info = asg_cluster.get("launchInfo")
            curr_image = None
            if asg_vm_info:
                curr_image = baseimages_helper.get_by_provider_name(request, asg_vm_info["imageId"])
                if asg_vm_info.get("subnets"):
                    asg_vm_info["subnetArrays"] = asg_vm_info["subnets"].split(',')
            group_info = asg_cluster.get("groupInfo")
            group_info = get_group_config_internal(group_info)
            #Directly search the cluster name. As long as there exists a cluster, treat this as cmp and hide
            #Launch Configuration
            basic_cluster_info = clusters_helper.get_cluster(request, group_name)
            if basic_cluster_info:
                is_cmp = True
        else:
            asg_vm_info = None
            group_info = None
            curr_image = None
            raise TeletraanException("Group does not exist. Please create capacity from the environments page.")

        pas_config = autoscaling_groups_helper.get_pas_config(request, group_name)
        return render(request, 'groups/asg_config.html', {
            "asg_vm_config": asg_vm_info,
            "app_names": appNames,
            "curr_image": curr_image,
            "group_config": group_info,
            "group_name": group_name,
            "pas_config": pas_config,
            "is_cmp": is_cmp,
            "disallow_autoscaling": _disallow_autoscaling(curr_image),
            "storage": get_messages(request)
        })


class GroupDetailView(View):
    def get(self, request, group_name):
        autoscaling_summary = autoscaling_groups_helper.get_autoscaling_summary(request, group_name)
        if autoscaling_summary is None:
            raise NotFoundException(f'Group {group_name} does not exist.')
        asg_status = autoscaling_summary.get("status", "UNKNOWN")
        enable_spot = autoscaling_summary.get("enableSpot", False)
        envs = environs_helper.get_all_envs_by_group(request, group_name)
        disabled_actions = autoscaling_groups_helper.get_disabled_asg_actions(request, group_name)
        pas_config = autoscaling_groups_helper.get_pas_config(request, group_name)
        base_metric_url = "https://statsboard.pinadmin.com/build?"

        group_size_url = base_metric_url+'''
            {"renderer":"line","title":"Fleet Size", "yAxisLabel":"Group Size", "ymin":"0","from":"1w",
             "metrics":[{"agg":"avg", "color":"dodgerblue","db":"tsdb", "dsValue":"10m", "renderer":"line",
                         "metric":"autoscaling.%s.size"}]}
        ''' % group_name

        for env in envs:
            env['launchlatencylink'] = base_metric_url + '''
                {"renderer":"line", "yAxisLabel":"Launch Latency","ymin":"0","from":"1w",
                 "metrics":[{"agg":"avg", "color":"dodgerblue","db":"tsdb", "dsValue":"10m", "renderer":"line",
                             "metric":"autoscaling.%s.%s.launchlatency"}]}
            ''' % (env.get('envName'), env.get('stageName'))

            env['deploylatencylink'] = base_metric_url + '''
                {"renderer":"line", "yAxisLabel":"Deploy Latency", "ymin":"0","from":"1w",
                 "metrics":[{"agg":"avg", "color":"dodgerblue","db":"tsdb", "dsValue":"10m", "renderer":"line",
                             "metric":"autoscaling.%s.%s.deploylatency"}]}
            ''' % (env.get('envName'), env.get('stageName'))

            env['deployfailedlink'] = base_metric_url + '''
                {"renderer":"line", "yAxisLabel":"Launch Failed", "ymin":"0","from":"1w",
                 "metrics":[{"agg":"mimmax", "color":"dodgerblue","db":"tsdb", "dsValue":"10m", "renderer":"line",
                             "metric":"autoscaling.%s.%s.first_deploy.failed"}]}
            ''' % (env.get('envName'), env.get('stageName'))

        if "Terminate" in disabled_actions:
            scaling_down_event_enabled = False
        else:
            scaling_down_event_enabled = True
        group_info = autoscaling_groups_helper.get_group_info(request, group_name)
        if group_info:
            launch_config = group_info.get('launchInfo')
            curr_image = baseimages_helper.get_by_provider_name(request, launch_config["imageId"])
        else:
            launch_config = None
            curr_image = None
        return render(request, 'groups/group_details.html', {
            "asg_status": asg_status,
            "enable_spot": enable_spot,
            "group_name": group_name,
            "scaling_down_event_enabled": scaling_down_event_enabled,
            "envs": envs,
            "group_info": group_info,
            "launch_config": launch_config,
            "pas_enabled": pas_config['pas_state'] if pas_config else False,
            "disallow_autoscaling": _disallow_autoscaling(curr_image),
            "group_size_url": group_size_url,
        })


# generate aws related settings
def get_aws_settings(request):
    params = request.GET
    app_name = params["app_name"]
    images = baseimages_helper.get_by_name(request, app_name, settings.DEFAULT_CELL)
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
    # hardcode the arch to be "x86_64" since this feature is only for very old clusters with cpu arch x86_64
    # thus, abandon this API https://github.com/pinternal/rodimus/pull/148/files which gets the host type list according to the current provider_name
    instance_types = hosttypes_helper.get_by_arch(request, "x86_64")
    host_types = []
    for instance_type in instance_types:
            host_types.append(instance_type['provider_name'])
    curr_instance_type = request.GET.get("curr_instance_type", "")
    contents = render_to_string("groups/get_hosttype.tmpl",
                                {"instance_types": host_types,
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
    placement_group = None
    asg_status = str(params['asgStatus']).upper()
    use_placement_group = False

    try:
        # Configure subnet to launch hosts
        if 'subnet' in params:
            subnet = params['subnet']
            # check if custom subnet is specified and custom placement group is in it.
            if 'customSubnet' in params and 'customPlacementGroup' in params and 'placementGroup' in params:
                # The check box is ticked and placement group is entered.
                placement_group = params['placementGroup']
                use_placement_group = True

        # When a group has an associated ASG, its status is either ENABLED or DISABLED.
        # We should always launch in ASG when an ASG is set up regardless its status,
        # unless placement group is specified.
        if (asg_status == 'ENABLED' or asg_status == 'DISABLED') and not use_placement_group:
            # Launch hosts inside ASG / Bump ASG size by required instances
            autoscaling_groups_helper.launch_hosts(request, group_name, num, None)
            content = 'Capacity increased by {} for Auto Scaling Group {}. Please go to ' \
                      '<a href="/groups/{}/">group page</a> ' \
                      'to check new hosts information.'.format(num, group_name, group_name)
            if 'customSubnet' in params:
                content += '\nNote: the subnet {} you selected was ignored.'.format(subnet)
            if asg_status == 'DISABLED':
                content += '\nNote: The ASG is currently DISABLED, so the instance you '\
                           'requested will be launched when ASG is ENABLED.'
            messages.add_message(request, messages.SUCCESS, content)
        else:
            # Launch hosts outside ASG / static hosts
            if not subnet:
                # No subnet specified show error message
                content = 'Failed to launch hosts to group {}. Please choose subnets in' \
                          ' <a href="/groups/{}/config/">group config</a>.' \
                          ' If you have any question, please contact your friendly Teletraan owners' \
                          ' for immediate assistance!'.format(group_name, group_name)
                messages.add_message(request, messages.ERROR, content)
            else:
                # Subnet is specified. Toggle based on presence of placement group param
                if placement_group is not None:
                    # Launch static hosts in the given PG
                    host_ids = autoscaling_groups_helper.launch_hosts_with_placement_group(
                        request, group_name, num, subnet, placement_group)
                else:
                    host_ids = autoscaling_groups_helper.launch_hosts(request, group_name, num, subnet)

                if len(host_ids) > 0:
                    content = '{} hosts have been launched to group {} (host ids: {})'.format(num, group_name, host_ids)
                    messages.add_message(request, messages.SUCCESS, content)
                else:
                    content = 'Failed to launch hosts to group {}. Please make sure the' \
                              ' <a href="/groups/{}/config/">group config</a>' \
                              ' is correct. If you have any question, please contact your friendly Teletraan owners' \
                              ' for immediate assistance!'.format(group_name, group_name)
                    messages.add_message(request, messages.ERROR, content)

    except Exception as e:
        messages.add_message(request, messages.ERROR, str(e))
        log.error(traceback.format_exc())
        raise

    return redirect('/groups/{}/'.format(group_name))


# Terminate all instances
def terminate_all_hosts(request, group_name):

    try:
        response = autoscaling_groups_helper.terminate_all_hosts(request, group_name)
        if response is not None and isinstance(response, dict):

            success_count = 0
            failed_count = 0
            failed_instance_ids = []

            content = "{} hosts were marked for termination \n".format(len(response))

            for id, status in response.items():
                if status == "UNKNOWN" or status == "FAILED":
                    failed_count += 1
                    failed_instance_ids.append(id)
                else:
                    success_count += 1

            content += "{} hosts successfully terminating...\n".format(success_count)
            content += "{} hosts failed to terminate \n".format(failed_count)
            content += ",".join(failed_instance_ids)

            messages.add_message(request, messages.SUCCESS, content)

        else:
            content = "Unexpected response from rodimus backend"
            messages.add_message(request, messages.ERROR, content)

    except Exception as e:
        messages.add_message(request, messages.ERROR, str(e))
        log.error(traceback.format_exc())
        raise

    return redirect('/groups/{}/'.format(group_name))


def instance_action_in_asg(request, group_name):
    host_id = request.GET.get("hostId", "")
    action = request.GET.get("action", "")
    host_ids = []
    host_ids.append(host_id)
    try:
        autoscaling_groups_helper.hosts_action_in_group(request, group_name, host_ids, action)
    except:
        log.error(traceback.format_exc())
        raise
    return redirect('/groups/{}/'.format(group_name))


def attach_instances(request, group_name):
    try:
        params = request.POST
        hosts = params.get("other_hosts")
        host_ids = hosts.split(',')
        autoscaling_groups_helper.hosts_action_in_group(request, group_name, host_ids, "ATTACH")
        return redirect('/groups/{}/'.format(group_name))
    except Exception:
        log.error(traceback.format_exc())
        return redirect('/groups/{}/'.format(group_name))


# Health Check related
def get_health_check_activities(request, group_name):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    health_checks = autoscaling_groups_helper.get_health_check_activities(request, group_name, index, size)
    asg_status = autoscaling_groups_helper.get_autoscaling_status(request, group_name)
    disabled_actions = autoscaling_groups_helper.get_disabled_asg_actions(request, group_name)
    if "Terminate" in disabled_actions:
        scaling_down_event_enabled = False
    else:
        scaling_down_event_enabled = True

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

def get_charts(request, group_name, type):

    if type == "az":
        return render(request, 'groups/distribution_charts.tmpl', get_host_az_dist(request, group_name))

    elif type == "ami":
        return render(request, 'groups/distribution_charts.tmpl', get_host_ami_dist(request, group_name))

    elif type == "host-type":
        return render(request, 'groups/distribution_charts.tmpl', get_host_type_dist(request, group_name))


def get_host_az_dist(request, group_name):
    aws_owner_id = accounts_helper.get_aws_owner_id_for_cluster_name(request, group_name)
    host_az_dist = hosts_helper.query_cmdb(
        query="tags.Autoscaling:{} AND state:running".format(group_name),
        fields="location", account_id=aws_owner_id
    )

    counter = Counter([x['location'] for x in host_az_dist.json()])
    labels = list(counter.keys())
    data = list(counter.values())
    total = sum(data)
    percentages = map(lambda x: round((x / total) * 100, 1), data)

    return {
        "chart_name": "AZ Distribution",
        "chart_type": "az",
        "group_name": group_name,
        'labels': labels,
        'data': data,
        'label_data_percentage': list(zip(labels, data, percentages)),
        'total': total
    }

def get_host_type_dist(request, group_name):
    aws_owner_id = accounts_helper.get_aws_owner_id_for_cluster_name(request, group_name)
    host_type_dist = hosts_helper.query_cmdb(
        query="tags.Autoscaling:{} AND state:running".format(group_name),
        fields="cloud.aws",
        account_id=aws_owner_id
    )

    counter = Counter([x['cloud.aws']['instanceType'] for x in host_type_dist.json()])
    labels = list(counter.keys())
    data = list(counter.values())
    total = sum(data)
    percentages = map(lambda x: round((x / total) * 100, 1), data)

    return {
        "chart_name": "Host Types Distribution",
        "chart_type": "host-type",
        "group_name": group_name,
        'labels': labels,
        'data': data,
        'label_data_percentage': list(zip(labels, data, percentages)),
        'total': total
    }

def get_host_ami_dist(request, group_name):
    cluster_config = clusters_helper.get_cluster(request, group_name)
    aws_owner_id = accounts_helper.get_aws_owner_id_for_cluster(request, cluster_config)
    host_ami_dist = hosts_helper.query_cmdb(
        query="tags.Autoscaling:{} AND state:running".format(group_name),
        fields="cloud.aws.imageId",
        account_id=aws_owner_id
    )

    counter = Counter([x['cloud.aws.imageId'] for x in host_ami_dist.json()])
    labels = list(counter.keys())
    data = list(counter.values())
    total = sum(data)
    percentages = map(lambda x: round((x / total) * 100, 1), data)

    current_AMI = baseimages_helper.get_by_id(request, cluster_config["baseImageId"])
    current_AMI = current_AMI["provider_name"]

    label_data_percentage = list(zip(labels, data, percentages))
    any_host_with_outdated_ami = False

    if len(label_data_percentage) > 1 or (len(label_data_percentage) == 1 and label_data_percentage[0][0] != current_AMI):
        any_host_with_outdated_ami = True

    return {
        "chart_name": "AMI Distribution",
        "chart_type": "ami",
        "group_name": group_name,
        'labels': labels,
        'data': data,
        'label_data_percentage': label_data_percentage,
        'total': total,
        'current_AMI': current_AMI,
        'any_host_with_outdated_ami': any_host_with_outdated_ami
    }


def get_health_check_details(request, id):
    health_check = autoscaling_groups_helper.get_health_check(request, id)
    env = environs_helper.get(request, health_check.get('env_id'))
    health_check['env_name'] = env.get('envName')
    health_check['stage_name'] = env.get('stageName')

    health_check_error = autoscaling_groups_helper.get_health_check_error(request, id)
    if health_check_error:
        env = environs_helper.get(request, health_check.get('env_id'))
        health_check_error['env_name'] = env.get('envName')
        health_check_error['stage_name'] = env.get('stageName')
        if IS_PINTEREST and PHOBOS_URL:
            from brood.client import Brood
            cmdb = Brood()
            host_ip = cmdb.get_query(query="id:" + health_check['host_id'],
                                     fields="config.internal_address")[0]['config.internal_address']
            if host_ip is not None:
                health_check_error['phobos_link'] = PHOBOS_URL + host_ip

    return render(request, 'groups/health_check_details.html', {
        "health_check": health_check,
        "health_check_error": health_check_error
    })


def create_manually_health_check(request, group_name):
    health_check_info = {}
    health_check_info["group_name"] = group_name
    health_check_info["type"] = "MANUALLY_TRIGGERED"
    autoscaling_groups_helper.create_health_check(request, group_name, health_check_info)
    return redirect('/groups/{}/health_check_activities/'.format(group_name))


def enable_scaling_down_event(request, group_name):
    autoscaling_groups_helper.enable_autoscaling(request, group_name)
    return redirect('/groups/{}/'.format(group_name))


def disable_scaling_down_event(request, group_name):
    autoscaling_groups_helper.disable_scaling_down_event(request, group_name)
    return redirect('/groups/{}/'.format(group_name))


def add_scheduled_actions(request, group_name):
    params = request.POST

    # validate scheduled capacity against ASG config minimum and maximum size
    asg_summary = autoscaling_groups_helper.get_autoscaling_summary(request, group_name)
    asg_minsize = int(asg_summary.get("minSize", -1))
    asg_maxsize = int(asg_summary.get("maxSize", -1)) # invalid value indicates no need to check

    if asg_minsize < 0 or asg_maxsize < 0:
        raise TeletraanException("ASG's Min Size and Max Size have invalid values. Please update the ASG config to make sure Min Size and Max Size >= 0")
    scheduled_action_capacity = int(params['capacity'])
    if scheduled_action_capacity < asg_minsize or scheduled_action_capacity > asg_maxsize:
        raise TeletraanException("Invalid capacity: {}. Desired capacity must be within the limits of ASG's minimum capacity ({}) and maximum capacity ({}). Please change the value you input for Capacity.".format(params['capacity'], asg_minsize, asg_maxsize))

    try:
        schedule_action = {}
        schedule_action['clusterName'] = group_name
        schedule_action['schedule'] = params['schedule']
        schedule_action['capacity'] = params['capacity']

        autoscaling_groups_helper.add_scheduled_actions(request, group_name, [schedule_action])
    except Exception:
        log.error(traceback.format_exc())
    return redirect("/groups/{}/config/".format(group_name))


def get_scheduled_actions(request, group_name):
    scheduled_actions = autoscaling_groups_helper.get_scheduled_actions(request, group_name)
    content = render_to_string("groups/asg_schedules.tmpl", {
        'group_name': group_name,
        'scheduled_actions': scheduled_actions,
        'csrf_token': get_token(request),
    })
    return HttpResponse(json.dumps(content), content_type="application/json")

def get_suspended_processes(request, group_name):
    suspended_processes = autoscaling_groups_helper.get_disabled_asg_actions(request, group_name)
    all_processes = autoscaling_groups_helper.get_available_scaling_process(request, group_name)
    all_processes.sort()

    process_suspended_status = []

    for p in all_processes:
        if p in suspended_processes:
            process_suspended_status.append({"name": p, "suspended": True})
        else:
            process_suspended_status.append({"name": p, "suspended": False})

    content = render_to_string("groups/asg_processes.tmpl", {
        'group_name': group_name,
        'process_suspended_status': process_suspended_status,
        'csrf_token': get_token(request),
    })
    return HttpResponse(json.dumps(content), content_type="application/json")

def suspend_process(request, group_name, process_name):
    update_request = {}
    update_request["suspend"] = [process_name]
    autoscaling_groups_helper.update_scaling_process(request, group_name, update_request)

    return get_suspended_processes(request, group_name)

def resume_process(request, group_name, process_name):
    update_request = {}
    update_request["resume"] = [process_name]
    autoscaling_groups_helper.update_scaling_process(request, group_name, update_request)

    return get_suspended_processes(request, group_name)

def delete_scheduled_actions(request, group_name):
    params = request.POST
    action_id = params['action_id']
    autoscaling_groups_helper.delete_scheduled_action(request, group_name, action_id)
    return get_scheduled_actions(request, group_name)


def _parse_actions_configs(query_data, group_name):
    page_data = dict(query_data.lists())
    configs = []
    for key, value in page_data.items():
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
        autoscaling_groups_helper.add_scheduled_actions(request, group_name, configs)
        return get_scheduled_actions(request, group_name)
    except Exception:
        log.error(traceback.format_exc())
        return HttpResponse(json.dumps({'content': ""}), content_type="application/json")


def get_terminating_hosts_by_group(request, group_name):
    data = groups_helper.get_terminating_by_group(request, group_name)
    return HttpResponse(json.dumps(data), content_type="application/json")
