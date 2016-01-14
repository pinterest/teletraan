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

from helpers import environs_helper
from helpers import images_helper, groups_helper
from helpers import specs_helper
from helpers import autoscaling_metrics_helper
from diff_match_patch import diff_match_patch

log = logging.getLogger(__name__)


DEFAULT_PAGE_SIZE = 50


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


def create_group(request):
    params = request.POST
    group_name = params["group_name"]
    try:
        groups_helper.create_group(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise
    return redirect("/groups/{}/".format(group_name))


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
        if group_info and group_info.get("subnets"):
            group_info["subnetArrays"] = group_info["subnets"].split(',')
        if group_info and not group_info.get("asgStatus"):
            group_info["asgStatus"] = "UNKNOWN"
        if group_info and group_info.get("launchLatencyTh"):
            group_info["launchLatencyTh"] = group_info.get("launchLatencyTh") / 60
        if group_info and group_info.get("healthcheckPeriod"):
            group_info["healthcheckPeriod"] = group_info.get("healthcheckPeriod") / 60

        appNames = images_helper.get_all_app_names(request)
        appNames = sorted(appNames)
        curr_image = images_helper.get_image_by_id(request, group_info["imageId"])
        html = render_to_string('groups/group_config.tmpl', {
            "group_name": group_name,
            "app_names": appNames,
            "config": group_info,
            "curr_image": curr_image,
            "csrf_token": get_token(request),
        })
    except:
        log.error(traceback.format_exc())
        raise
    return HttpResponse(json.dumps(html), content_type="application/json")


def update_launch_config(request, group_name):
    try:
        params = request.POST
        launchRequest = {}
        launchRequest["groupName"] = group_name
        launchRequest["instanceType"] = params["instanceType"]
        launchRequest["securityGroup"] = params["securityGroup"]
        launchRequest["imageId"] = params["imageId"]
        launchRequest["userData"] = params["userData"]
        launchRequest["chatroom"] = params["chatroom"]
        launchRequest["watchRecipients"] = params["watch_recipients"]
        launchRequest["emailRecipients"] = params["email_recipients"]
        launchRequest["pagerRecipients"] = params["pager_recipients"]
        launchRequest["launchLatencyTh"] = int(params["launch_latency_th"]) * 60
        launchRequest["iamRole"] = params["iam_role"]
        launchRequest["subnets"] = ",".join(params.getlist("subnets"))
        launchRequest["asgStatus"] = params["asg_status"]

        if params["assignPublicIP"] == "True":
            launchRequest["assignPublicIp"] = True
        else:
            launchRequest["assignPublicIp"] = False

        if "healthcheck_state" in params:
            launchRequest["healthcheckState"] = True
        else:
            launchRequest["healthcheckState"] = False
        launchRequest["healthcheckPeriod"] = int(params["healthcheck_period"]) * 60
        groups_helper.update_group_info(request, group_name,  launchRequest)
        return get_launch_config(request, group_name)
    except:
        log.error(traceback.format_exc())
        raise


def gen_asg_setting(request, group_name):
    asg = groups_helper.get_autoscaling(request, group_name)
    policies = groups_helper.TerminationPolicy
    content = render_to_string("groups/create_asg_modal.tmpl", {
        "asg": asg,
        "group_name": group_name,
        "policies": policies,
        "csrf_token": get_token(request)})
    return HttpResponse(content)


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
    asg = groups_helper.get_autoscaling(request, group_name)
    instances = groups_helper.get_group_instances(request, group_name)
    group_size = len(instances)
    policies = groups_helper.TerminationPolicy
    content = render_to_string("groups/asg_config.tmpl", {
        "group_name": group_name,
        "asg": asg,
        "group_size": group_size,
        "terminationPolicies": policies,
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
                                                    "coolDown": make_int(params["scaleupCooldownTime"])})
        scaling_policies["scaledownPolicies"].append({"scaleSize": make_int(params["scaledownSize"]),
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
            alarm_info["threshold"] = int(page_data["threshold_{}".format(alarm_id)][0])
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
        alarm_info["threshold"] = int(params["threshold"])
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
        print alarm_info
        groups_helper.add_alarm(request, group_name, [alarm_info])
    except:
        log.error(traceback.format_exc())

    return redirect("/groups/{}/config/".format(group_name))


# group host information
def get_group_info(request, group_name):
    try:
        group_info = groups_helper.get_group_info(request, group_name)
        hosts_in_asg = set(groups_helper.get_autoscaling_group_instances(request, group_name))
        all_hosts_in_group = groups_helper.get_group_instances(request, group_name)
        non_asg_host_names = []
        asg_host_names = []
        for host in all_hosts_in_group:
            host_name = host.get("hostName", "")
            host_id = host.get("hostId", "")
            if host_id in hosts_in_asg:
                asg_host_names.append(host_name)
                hosts_in_asg.remove(host_id)
            else:
                non_asg_host_names.append(host_name)
        asg_host_names.extend(hosts_in_asg)

        if group_info["asgStatus"] == "DISABLED":
            asg_status_str = "Disabled"
        elif group_info["asgStatus"] == "ENABLED":
            asg_status_str = "Enabled"
        else:
            asg_status_str = "Not Enabled"
        group_size = len(hosts_in_asg) + len(all_hosts_in_group)
        content = render_to_string("groups/group_info.tmpl", {
            "group_name": group_name,
            "instance_type": group_info["instanceType"],
            "security_group": group_info["securityGroup"],
            "fleet_size": group_size,
            "asg_status": asg_status_str,
            "asg_hosts": asg_host_names,
            "other_hosts": non_asg_host_names
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
    token = ""
    if params.get("token"):
        token = params["token"]

    scaling_activities_info = groups_helper.get_scaling_activities(request, group_name,
                                                                   50, "")
    activities = scaling_activities_info["activities"]
    next_token = scaling_activities_info["next_token"]
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
    config = groups_helper.get_group_info(request, groupName)
    empty_config = False
    if not config.get("instanceType") or not config.get("securityGroup") \
            or not config.get("imageId") or not config.get("userData") or not config.get("subnets"):
        empty_config = True

    instance_types, sorted_subnets, sorted_sgs = get_system_specs(request)
    if config and config.get("subnets"):
        config["subnetArrays"] = config["subnets"].split(',')

    if config and config.get("userData"):
        config["userData"] = config["userData"].replace("\n", "<br>")

    contents = render_to_string('groups/get_config.tmpl', {
        "groupName": groupName,
        "empty_config": empty_config,
        "subnets": sorted_subnets,
        "security_groups": sorted_sgs,
        "config": config,
        "asgStatus": config["asgStatus"],
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
        asg_status = groups_helper.get_autoscaling_status(request, group_name)

        return render(request, 'groups/asg_config.html', {
            "asg_status": asg_status,
            "group_name": group_name,
        })


class GroupDetailView(View):
    def get(self, request, group_name):
        asg_status = groups_helper.get_autoscaling_status(request, group_name)
        group_size_datum = autoscaling_metrics_helper.get_asg_size_metric(request, group_name, "1d-ago")
        alarm_infos = groups_helper.get_alarms(request, group_name)
        enable_policy = False
        if alarm_infos and len(alarm_infos) > 0:
            enable_policy = True

        removeIdx = []

        if alarm_infos:
            for idx in xrange(len(alarm_infos)):
                alarm_info = alarm_infos[idx]
                alarm_infos[idx]["actionType2"] = "UNKNOWN"
                alarm_infos[idx]["threshold2"] = -1
                metric_name = alarm_info["metricSource"]
                if idx > 0 and metric_name == alarm_infos[idx - 1]["metricSource"]:
                    alarm_infos[idx - 1]["actionType2"] = alarm_info["actionType"]
                    alarm_infos[idx - 1]["threshold2"] = alarm_info["threshold"]
                    removeIdx.append(idx)
                else:
                    alarm_info["metric_datum"] = autoscaling_metrics_helper.get_metric_data(request, group_name,
                                                                                            metric_name, "1d-ago")

        for offset, idx in enumerate(removeIdx):
            idx -= offset
            del alarm_infos[idx]

        envs = environs_helper.get_all_envs_by_group(request, group_name)
        scaling_down_event_enabled = groups_helper.get_scaling_down_event_status(request, group_name)
        return render(request, 'groups/group_details.html', {
            "asg_status": asg_status,
            "group_name": group_name,
            "enable_policy": enable_policy,
            "alarm_infos": alarm_infos,
            "group_size_datum": group_size_datum,
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
    asgStatus = params["asgStatus"]
    instanceCnt = int(params["instanceCnt"])
    subnet = ""
    try:
        if asgStatus == 'ENABLED':
            groups_helper.launch_instance_in_group(request, group_name, instanceCnt, subnet)
            content = 'Capacity increased by {} for Auto Scaling Group {}. Please go to ' \
                      '<a href="https://deploy.pinadmin.com/groups/{}/">group page</a> ' \
                      'to check new hosts information.'.format(instanceCnt, group_name, group_name)
            messages.add_message(request, messages.SUCCESS, content)
        elif asgStatus == 'DISABLED':
            content = 'This Auto Scaling Group {} is disabled.' \
                      ' Please go to <a href="https://deploy.pinadmin.com/groups/{}/config/">group config</a>' \
                      ' to enable it.'.format(group_name, group_name)
            messages.add_message(request, messages.ERROR, content)
        else:
            if "subnet" in params:
                subnet = params["subnet"]
            instanceIds = groups_helper.launch_instance_in_group(request, group_name, instanceCnt,
                                                                 subnet)
            if len(instanceIds) > 0:
                content = '{} instances have been launched to group {} (instance ids: {})' \
                    .format(instanceCnt, group_name, instanceIds)
                messages.add_message(request, messages.SUCCESS, content)
            else:
                content = 'Failed to launch instances to group {}. Please make sure the' \
                          ' <a href="https://deploy.pinadmin.com/groups/{}/config/">group config</a>' \
                          ' is correct. If you have any question, please contact your friendly Teletraan owners' \
                          ' for immediate assistance!'.format(group_name, group_name)
                messages.add_message(request, messages.ERROR, content)
    except:
        log.error(traceback.format_exc())
        raise
    return redirect('/groups/{}'.format(group_name))


# Detach/Attach instances from asg
def detach_instance_from_asg(request, group_name):
    host_id = request.GET.get("hostId", "")
    host_ids = []
    host_ids.append(host_id)
    try:
        groups_helper.detach_instance_in_group(request, group_name, host_ids)
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
