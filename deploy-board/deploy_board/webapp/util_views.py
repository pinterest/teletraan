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
"""Collection of all helper utils
"""
from deploy_board.settings import SITE_METRICS_CONFIGS, TELETRAAN_SERVICE_HEALTHCHECK_URL
from django.template.loader import render_to_string
from django.http import HttpResponse
import json
import urllib2
from deploy_board import settings
from helpers import environs_helper
from helpers import autoscaling_metrics_helper, autoscaling_groups_helper
import traceback
import logging

log = logging.getLogger(__name__)

# convert json opentsdb to array based
def _convert_opentsdb_data(dps):
    data = []
    for key, value in dps.iteritems():
        data.append([int(key), value])
    data.sort(key=lambda x: x[0])
    return data


def _get_latest_metrics(url):
    response = urllib2.urlopen(url)
    data = json.loads(response.read())
    # Return the first datapoint in the datapoints list
    if data:
        if 'datapoints' in data[0] and len(data[0]['datapoints']) != 0:
            return data[0]['datapoints']
            # Check for TSDB response
        if 'dps' in data[0] and len(data[0]['dps']) != 0:
            return _convert_opentsdb_data(data[0]['dps'])
    return 0


def get_service_metrics(request, name, stage):
    metrics = environs_helper.get_env_metrics_config(request, name, stage)
    data = {}
    for metric in metrics:
        data[metric['title']] = _get_latest_metrics(metric['url'])
    return HttpResponse(json.dumps({'html': data}), content_type="application/json")


def get_site_health_metrics(request):
    data = {}
    for metric in SITE_METRICS_CONFIGS:
        data[metric['title']] = _get_latest_metrics(metric['url'])
    return HttpResponse(json.dumps({'html': data}), content_type="application/json")


def _get_latest_alarm(url):
    response = urllib2.urlopen(url)
    data = json.loads(response.read())
    # assume only return one alarm
    return data.itervalues().next()


def get_service_alarms(request, name, stage):
    alarm_configs = environs_helper.get_env_alarms_config(request, name, stage)
    alarms = {}
    for alarm in alarm_configs:
        value = _get_latest_alarm(alarm['alarmUrl'])
        if value and value["triggered"]:
            alarms[alarm['name']] = value
    html = render_to_string('configs/alarm_details.tmpl', {
        "alarms": alarms,
        "hasAlarm": True if alarms else False,
    })
    return HttpResponse(html)


def validate_metrics_url(request):
    url = request.POST['newEntryValue']
    response = urllib2.urlopen(url)
    data = json.loads(response.read())
    if len(data) > 0:
        data = data[0]
        if 'datapoints' in data.keys() or 'dps' in data.keys():
            return HttpResponse(json.dumps({'result': True}), content_type="application/json")
    return HttpResponse(json.dumps({'result': False}), content_type="application/json")


def _get_backend_health():
    response = urllib2.urlopen(TELETRAAN_SERVICE_HEALTHCHECK_URL)
    return json.loads(response.read())


def health_check(request):
    try:
        # The fact that we can call backend health check, and return,
        # suggest UI, backend and mysql all in relatively good health
        result = _get_backend_health()
        if result:
            return HttpResponse("OK", content_type="text/plain")
    except Exception:
        pass
    return HttpResponse("FAILED", status=500, content_type="text/plain")


def loggedout(request):
    return HttpResponse("Goodbye!", content_type="text/plain")

def get_latency_metrics(request, group_name):
    envs = environs_helper.get_all_envs_by_group(request, group_name)
    launch_config = autoscaling_groups_helper.get_group_info(request, group_name)
    util_data = {}
    stage_names = []
    if len(envs) == 0:
        return HttpResponse(json.dumps(util_data), content_type="application/json")

    try:
        for env in envs:
            name = "{}.{}".format(env["envName"], env["stageName"])
            stage_names.append(name)
            metric_name1 = "launch_latency.{}".format(name)
            launch_data_points = autoscaling_metrics_helper.get_latency_data(request, env["id"],
                                                                             "LAUNCH", settings.DEFAULT_START_TIME)
            json_data = []
            for data_point in launch_data_points:
                timestamp, value = data_point["timestamp"], data_point["value"] / 1000
                json_data.append([timestamp, value])
            util_data[metric_name1] = json_data

            metric_name2 = "deploy_latency.{}".format(name)
            deploy_data_points = autoscaling_metrics_helper.get_latency_data(request, env["id"],
                                                                             "DEPLOY", settings.DEFAULT_START_TIME)
            json_data2 = []
            for data_point in deploy_data_points:
                timestamp, value = data_point["timestamp"], data_point["value"] / 1000
                json_data2.append([timestamp, value])
            util_data[metric_name2] = json_data2

        util_data["stage_names"] = stage_names
        util_data["launch_latency_th"] = launch_config.get("groupInfo")["launchLatencyTh"]
    except:
        log.error(traceback.format_exc())
    return HttpResponse(json.dumps(util_data), content_type="application/json")


def get_launch_rate(request, group_name):
    envs = environs_helper.get_all_envs_by_group(request, group_name)
    util_data = {}
    if len(envs) == 0:
        return HttpResponse(json.dumps(util_data), content_type="application/json")

    try:
        util_data["metric_names"] = []
        for env in envs:
            metric_name = "mimmax:autoscaling.{}.{}.first_deploy.failed".format(
                env["envName"], env["stageName"])
            rate_data_points = autoscaling_metrics_helper.get_raw_metrics(request, metric_name,
                                                                          settings.DEFAULT_START_TIME)
            json_data = []
            for data_point in rate_data_points:
                timestamp, value = data_point["timestamp"], data_point["value"]
                json_data.append([timestamp, value])

            util_data[metric_name] = json_data
            util_data["metric_names"].append(metric_name)
    except:
        log.error(traceback.format_exc())
    return HttpResponse(json.dumps(util_data), content_type="application/json")


def get_pas_metrics(request, group_name):
    pas_config = autoscaling_groups_helper.get_pas_config(request, group_name)
    util_data = {}
    if pas_config['pas_state'] != 'ENABLED':
        return HttpResponse(json.dumps(util_data), content_type="application/json")
    try:
        arcee_size_points = autoscaling_metrics_helper.get_pas_metrics(request, group_name,
                                                                       settings.DEFAULT_START_TIME, 'PREDICTED')


        json_data3 = []
        for data_point in arcee_size_points:
            timestamp, value = data_point["timestamp"], data_point["value"]
            json_data3.append([timestamp, value])
        util_data['arcee'] = json_data3
    except:
        log.error(traceback.format_exc())
    return HttpResponse(json.dumps(util_data), content_type="application/json")



