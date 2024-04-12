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

from django.shortcuts import render
from django.http import HttpResponse
from django.views.generic import View
import logging
from .common import is_agent_failed
from .helpers import environs_helper, agents_helper, autoscaling_groups_helper
from .helpers import environ_hosts_helper, hosts_helper
from deploy_board.settings import IS_PINTEREST, CMDB_API_HOST, CMDB_INSTANCE_URL, CMDB_UI_HOST, PHOBOS_URL
from datetime import datetime
import pytz
import requests
from . import common
requests.packages.urllib3.disable_warnings()

log = logging.getLogger(__name__)


def get_agent_wrapper(request, hostname):
    # gather the env name and stage info
    agents = agents_helper.get_agents_by_host(request, hostname)
    agent_wrappers = {'sidecars': [], 'services': []}
    is_unreachable = False
    for agent in agents:
        agent_wrapper = {}
        agent_wrapper["agent"] = agent
        envId = agent['envId']
        agent_env = environs_helper.get(request, envId)
        agent_wrapper["env"] = agent_env
        agent_wrapper["error"] = ""
        if agent.get('lastErrno', 0) != 0:
            agent_wrapper["error"] = agents_helper.get_agent_error(request, agent_env['envName'],
                                                                   agent_env['stageName'], hostname)
        if agent['state'] == 'UNREACHABLE':
            is_unreachable = True
        if agent_env and agent_env['systemPriority'] and agent_env['systemPriority'] > 0:
            agent_wrappers['sidecars'].append(agent_wrapper)
        else:
            agent_wrappers['services'].append(agent_wrapper)
    
    agent_wrappers['sidecars'] = sorted(agent_wrappers['sidecars'], key=lambda x: x["agent"]['lastUpdateDate'])
    agent_wrappers['services'] = sorted(agent_wrappers['services'], key=lambda x: x["agent"]['lastUpdateDate'])
    return agent_wrappers, is_unreachable


# TODO deprecated it
def get_asg_name(request, hosts):
    if IS_PINTEREST:
        for host in hosts:
            if host and host.get('groupName'):
                group_info = autoscaling_groups_helper.get_group_info(request, host.get('groupName'))
                if group_info and group_info.get("launchInfo") and group_info.get("launchInfo")["asgStatus"] == "ENABLED":
                    return host.get('groupName')
    return None


def get_show_terminate(hosts):
    for host in hosts:
        if host and host.get('state') and host.get('state') not in {'PENDING_TERMINATE', 'TERMINATING', 'TERMINATED', 'PENDING_TERMINATE_NO_REPLACE'}:
            return True
    return False

def should_display_reset_all_environments(hosts):
    for host in hosts:
        if host and host.get('state') and host.get('state') not in {'PENDING_TERMINATE', 'TERMINATING', 'TERMINATED', 'PENDING_TERMINATE_NO_REPLACE'}:
            return True
    return False

def get_host_id(hosts):
    if hosts:
        return hosts[0].get('hostId')
    return None

def get_account_id(hosts):
    for host in hosts:
        if host and host.get('accountId') and host.get('accountId') not in {'NULL', 'null'}:
            return host.get('accountId')
    return None


def _get_cloud(json_obj):
    try:
        return json_obj.get('cloud', None).get('aws', None)
    except:
        return None


def get_host_details(host_id):
    if not host_id:
        return None
    host_url = CMDB_API_HOST + CMDB_INSTANCE_URL + host_id
    response = requests.get(host_url)

    try:
        instance = response.json()
    except:
        # the host not found in CMDB
        return None

    cloud_info = _get_cloud(instance)
    if not cloud_info:
        return None

    launch_time = cloud_info.get('launchTime', 0)
    launch_time = datetime.fromtimestamp(launch_time / 1000, pytz.timezone('America/Los_Angeles')).strftime("%Y-%m-%d %H:%M:%S")
    availability_zone = cloud_info.get('placement', {}).get('availability_zone', None)
    ami_id = cloud_info.get('image_id', None)
    host_details = {
     'Subnet Id': instance.get('subnet_id', None),
     'State': instance.get('state', None),
     'Security Groups': instance.get('security_groups', None),
     'Availability Zone': availability_zone,
     'Tags': instance['tags'],
     'Launch Time': launch_time,
     'AMI Id': ami_id,
    }
    if IS_PINTEREST and PHOBOS_URL:
        host_ip = instance['config']['internal_address']
        host_name = instance['config']['name']
        if host_ip is not None:
            phobos_link = PHOBOS_URL + host_name
            host_details['Phobos Link'] = phobos_link
    return host_details


class GroupHostDetailView(View):
    def get(self, request, groupname, hostname):
        hosts = hosts_helper.get_hosts_by_name(request, hostname)
        host_id = get_host_id(hosts)
        account_id = get_account_id(hosts)
        asg = get_asg_name(request, hosts)

        show_terminate = get_show_terminate(hosts)
        show_reset_all_environments = should_display_reset_all_environments(hosts)
        show_warning_message = not show_terminate
        agent_wrappers, is_unreachable = get_agent_wrapper(request, hostname)
        host_details = get_host_details(host_id)

        return render(request, 'hosts/host_details.html', {
            'group_name': groupname,
            'hostname': hostname,
            'hosts': hosts,
            'host_id': host_id,
            'account_id': account_id,
            'agent_wrappers': agent_wrappers,
            'show_warning_message': show_warning_message,
            'show_reset_all_environments': show_reset_all_environments,
            'asg_group': asg,
            'is_unreachable': is_unreachable,
            'pinterest': IS_PINTEREST,
            'host_information_url': CMDB_UI_HOST,
            'host_details': host_details,
        })


class HostDetailView(View):
    def get(self, request, name, stage, hostname):
        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        duplicate_stage = ''
        for stage_name in stages:
            if stage_name != stage:
                hosts = environs_helper.get_env_capacity(request, name, stage_name, capacity_type="HOST")
                if hostname in hosts:
                    duplicate_stage = stage_name

        hosts = environ_hosts_helper.get_host_by_env_and_hostname(request, name, stage, hostname)
        host_id = get_host_id(hosts)
        account_id = get_account_id(hosts)
        show_terminate = get_show_terminate(hosts)
        show_reset_all_environments = should_display_reset_all_environments(hosts)
        show_warning_message = not show_terminate
        asg = get_asg_name(request, hosts)
        is_protected = False
        if asg:
            is_protected = autoscaling_groups_helper.is_hosts_protected(request, asg, [host_id])

        agent_wrappers, is_unreachable = get_agent_wrapper(request, hostname)
        has_failed_sidecars_or_host_env_sidecars = any(
            wrapper for wrapper in agent_wrappers['sidecars']
            if (wrapper['env'] and wrapper['env']['envName'] and
            wrapper['env']['stageName'] and
            wrapper['env']['envName'] == name and
            wrapper['env']['stageName'] == stage) or
            is_agent_failed(wrapper['agent'])
        )
        
        host_details = get_host_details(host_id)

        termination_limit = environs_helper.get_env_by_stage(request, name, stage).get('terminationLimit')

        return render(request, 'hosts/host_details.html', {
            'env_name': name,
            'stage_name': stage,
            'hostname': hostname,
            'hosts': hosts,
            'host_id': host_id,
            'account_id': account_id,
            'agent_wrappers': agent_wrappers,
            'show_terminate': show_terminate,
            'show_reset_all_environments': show_reset_all_environments,
            'show_warning_message': show_warning_message,
            'show_force_terminate': IS_PINTEREST,
            'asg_group': asg,
            'is_unreachable': is_unreachable,
            'pinterest': IS_PINTEREST,
            'host_information_url': CMDB_UI_HOST,
            'instance_protected': is_protected,
            'host_details': host_details,
            'duplicate_stage': duplicate_stage,
            'termination_limit': termination_limit,
            'has_failed_sidecars_or_host_env_sidecars': has_failed_sidecars_or_host_env_sidecars,
        })


def hosts_list(request):
    return render(request, 'hosts/hosts_landing.html', {
    })


def hosts_show(request, build_id):
    return HttpResponse("NOT IMPLEMENTED")
