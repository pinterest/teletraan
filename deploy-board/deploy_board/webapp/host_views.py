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
from helpers import environs_helper, agents_helper
from helpers import groups_helper, environ_hosts_helper, hosts_helper
from deploy_board.settings import IS_PINTEREST

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


def get_asg_name(request, host):
    asg = ''
    if host and host.get('groupName'):
        group_info = groups_helper.get_group_info(request, host.get('groupName'))
        if group_info and group_info["asgStatus"] == "ENABLED":
            asg = host.get('groupName')
    return asg


def get_show_terminate(host):
    if host and host.get('state') and host.get('state') != 'PENDING_TERMINATE' and host.get('state') != 'TERMINATING' and host.get('state') != 'TERMINATED':
        return True
    else:
        return False


class GroupHostDetailView(View):
    def get(self, request, groupname, hostname):
        hosts = hosts_helper.get_hosts_by_name(request, hostname)
        show_host = None
        for host in hosts:
            if host.get('groupName') == groupname:
                show_host = host
        asg = get_asg_name(request, show_host)
        agent_wrappers, is_unreachable = get_agent_wrapper(request, hostname)
        return render(request, 'hosts/host_details.html', {
            'group_name': groupname,
            'hostname': hostname,
            'host': show_host,
            'agent_wrappers': agent_wrappers,
            'asg_group': asg,
            'is_unreachable': is_unreachable,
            'pinterest': IS_PINTEREST,
        })


class HostDetailView(View):
    def get(self, request, name, stage, hostname):
        host = environ_hosts_helper.get_host_by_env_and_hostname(request, name, stage, hostname)
        show_terminate = get_show_terminate(host)

        # TODO deprecated it
        asg = get_asg_name(request, host)

        agent_wrappers, is_unreachable = get_agent_wrapper(request, hostname)
        return render(request, 'hosts/host_details.html', {
            'env_name': name,
            'stage_name': stage,
            'hostname': hostname,
            'host': host,
            'agent_wrappers': agent_wrappers,
            'show_terminate': show_terminate,
            'show_force_terminate': True,
            'asg_group': asg,
            'is_unreachable': is_unreachable,
            'pinterest': IS_PINTEREST,
        })


def hosts_list(request):
    return render(request, 'hosts/hosts_landing.html', {
    })


def hosts_show(request, build_id):
    return HttpResponse("NOT IMPLEMENTED")
