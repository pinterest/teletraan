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

from django.shortcuts import render, redirect
from django.http import HttpResponse
from django.views.generic import View
import traceback
import logging
from helpers import environs_helper, agents_helper, environ_hosts_helper
from helpers import groups_helper, hosts_helper, clusters_helper
from deploy_board.settings import IS_PINTEREST

log = logging.getLogger(__name__)


class HostDetailView(View):
    def get(self, request, name, stage, hostname):
        agents = agents_helper.get_agents_by_host(request, hostname)
        env = environs_helper.get_env_by_stage(request, name, stage)
        host = environ_hosts_helper.get_host_by_env_and_hostname(request, name, stage, hostname)
        show_terminate = False
        asg = ''
        if host and host.get('hostId'):
            if host.get('state') != 'PENDING_TERMINATE' and host.get('state') != 'TERMINATING' and host.get('state') != 'TERMINATED':
                show_terminate = True

        cluster_provider = clusters_helper.get_cluster_provider(request, name, stage)
        if cluster_provider == 'null':
            cluster_provider = None

        # TODO deprecated it
        if host and host.get('groupName'):
            group_info = groups_helper.get_group_info(request, host.get('groupName'))
            if group_info and group_info["asgStatus"] == "ENABLED":
                asg = host.get('groupName')

        # gather the env name and stage info
        agent_wrappers = []
        for agent in agents:
            agent_wrapper = {}
            agent_wrapper["agent"] = agent
            envId = agent['envId']
            agent_env = environs_helper.get(request, envId)
            agent_wrapper["env"] = env
            agent_wrapper["error"] = ""
            if agent.get('lastErrno', 0) != 0:
                agent_wrapper["error"] = agents_helper.get_agent_error(request, agent_env['envName'],
                                                                       agent_env['stageName'], hostname)
            agent_wrappers.append(agent_wrapper)

        return render(request, 'hosts/host_details.html', {
            'env_name': name,
            'stage_name': stage,
            'hostname': hostname,
            'host': host,
            'agent_wrappers': agent_wrappers,
            'show_terminate': show_terminate,
            'cluster_provider': cluster_provider,
            'asg_group': asg,
            'pinterest': IS_PINTEREST,
        })


# TODO deprecated it
def get_host_details(request, name):
    agents = agents_helper.get_agents_by_host(request, name)
    hosts = hosts_helper.get_hosts_by_name(request, name)
    # gather the env name and stage info
    agent_wrappers = []
    for agent in agents:
        agent_wrapper = {}
        agent_wrapper["agent"] = agent
        envId = agent['envId']
        env = environs_helper.get(request, envId)
        agent_wrapper["env"] = env
        agent_wrapper["error"] = ""
        if agent.get('lastErrno', 0) != 0:
            agent_wrapper["error"] = agents_helper.get_agent_error(request, env['envName'],
                                                                   env['stageName'], name)
        agent_wrappers.append(agent_wrapper)

    host_id = ""
    asgGroup = ""
    for host in hosts:
        host_id = host['hostId']
        group_name = host["groupName"]
        # TODO Remove this hack
        if not group_name or group_name == 'NULL':
            continue
        group_info = groups_helper.get_group_info(request, group_name)
        if group_info and group_info["asgStatus"] == "ENABLED":
            asgGroup = group_name
            break

    return render(request, 'hosts/host_details.html', {
        'agent_wrappers': agent_wrappers,
        'hosts': hosts,
        'name': name,
        'hostId': host_id,
        'show_terminate': True,
        "asg_group": asgGroup,
        "pinterest": IS_PINTEREST,
    })


# TODO deprecated it
def terminate_host(request, name, stage, hostname):
    hostId = request.GET.get('hostId')
    params = request.POST
    decreaseSize = False
    if "checkToDecrease" in params:
        decreaseSize = True
    try:
        groups_helper.terminate_host_in_group(request, hostId, decreaseSize)
    except:
        log.error(traceback.format_exc())
        raise
    return redirect('/env/{}/{}/host/{}'.format(name, stage, hostname))


def hosts_list(request):
    return render(request, 'hosts/hosts_landing.html', {
    })


def hosts_show(request, build_id):
    return HttpResponse("NOT IMPLEMENTED")


# TODO deprecated it
def terminate_instance(request, name):
    hostId = request.GET.get('hostId')
    params = request.POST
    decreaseSize = False
    if "checkToDecrease" in params:
        decreaseSize = True
    try:
        groups_helper.terminate_host_in_group(request, hostId, decreaseSize)
    except:
        log.error(traceback.format_exc())
        raise
    return redirect('/host/{}'.format(name))
