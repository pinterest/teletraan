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
from helpers import environs_helper, agents_helper, autoscaling_groups_helper
from helpers import environ_hosts_helper, hosts_helper
from deploy_board.settings import IS_PINTEREST, TELETRAAN_HOST_INFORMATION_URL
from datetime import datetime
import pytz
import requests

log = logging.getLogger(__name__)


def get_agent_wrapper(request, hostname):
    # gather the env name and stage info
    agents = agents_helper.get_agents_by_host(request, hostname)
    agent_wrappers = []
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

        agent_wrappers.append(agent_wrapper)
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
        if host and host.get('state') and host.get('state') != 'PENDING_TERMINATE' and host.get('state') != 'TERMINATING' and host.get('state') != 'TERMINATED':
            return True
    return False


def get_host_id(hosts):
    if hosts:
        return hosts[0].get('hostId')
    return None


def get_host_details(host_id):
    host_url = TELETRAAN_HOST_INFORMATION_URL + '/api/cmdb/getinstance/' + host_id
    response = requests.get(host_url)
    instance = response.json()
    launch_time = instance['cloud']['aws']['launchTime']
    launch_time = datetime.fromtimestamp(launch_time / 1000, pytz.timezone('America/Los_Angeles')).strftime("%Y-%m-%d %H:%M:%S")
    host_details = {
     'Subnet Id': instance['subnet_id'],
     'State': instance['state'],
     'Security Groups': instance['security_groups'],
     'Availability Zone': instance['cloud']['aws']['placement']['availability_zone'],
     'Tags': instance['tags'],
     'Launch Time': launch_time,
     'AMI Id': instance['facts']['ec2_ami_id'],
    }
    return host_details


class GroupHostDetailView(View):
    def get(self, request, groupname, hostname):
        hosts = hosts_helper.get_hosts_by_name(request, hostname)
        host_id = get_host_id(hosts)
        asg = get_asg_name(request, hosts)

        show_terminate = get_show_terminate(hosts)
        show_warning_message = not show_terminate
        agent_wrappers, is_unreachable = get_agent_wrapper(request, hostname)
        host_details = get_host_details(host_id)

        return render(request, 'hosts/host_details.html', {
            'group_name': groupname,
            'hostname': hostname,
            'hosts': hosts,
            'host_id': host_id,
            'agent_wrappers': agent_wrappers,
            'show_warning_message': show_warning_message,
            'asg_group': asg,
            'is_unreachable': is_unreachable,
            'pinterest': IS_PINTEREST,
            'host_information_url': TELETRAAN_HOST_INFORMATION_URL,
            'host_details': host_details,
        })


class HostDetailView(View):
    def get(self, request, name, stage, hostname):
        hosts = environ_hosts_helper.get_host_by_env_and_hostname(request, name, stage, hostname)
        host_id = get_host_id(hosts)
        show_terminate = get_show_terminate(hosts)
        show_warning_message = not show_terminate
        asg = get_asg_name(request, hosts)
        is_protected = False
        if asg:
            is_protected = autoscaling_groups_helper.is_hosts_protected(request, asg, [host_id])

        agent_wrappers, is_unreachable = get_agent_wrapper(request, hostname)
        host_details = get_host_details(host_id)
        
        return render(request, 'hosts/host_details.html', {
            'env_name': name,
            'stage_name': stage,
            'hostname': hostname,
            'hosts': hosts,
            'host_id': host_id,
            'agent_wrappers': agent_wrappers,
            'show_terminate': show_terminate,
            'show_warning_message': show_warning_message,
            'show_force_terminate': IS_PINTEREST,
            'asg_group': asg,
            'is_unreachable': is_unreachable,
            'pinterest': IS_PINTEREST,
            'host_information_url': TELETRAAN_HOST_INFORMATION_URL,
            'instance_protected': is_protected,
            'host_details': host_details,
        })


def hosts_list(request):
    return render(request, 'hosts/hosts_landing.html', {
    })


def hosts_show(request, build_id):
    return HttpResponse("NOT IMPLEMENTED")
