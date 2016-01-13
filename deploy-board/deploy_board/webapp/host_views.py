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
from helpers import environs_helper, agents_helper
from helpers import groups_helper, hosts_helper
from deploy_board.settings import IS_PINTEREST

log = logging.getLogger(__name__)


class HostDetailView(View):
    def get(self, request, name):
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


def hosts_list(request):
    return render(request, 'hosts/hosts_landing.html', {
    })


def hosts_show(request, build_id):
    return HttpResponse("NOT IMPLEMENTED")


# terminate instance
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
