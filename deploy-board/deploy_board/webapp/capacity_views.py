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
"""Collection of all env promote config views
"""
import json
from django.http import HttpResponse
from django.middleware.csrf import get_token
from django.shortcuts import render
from django.template.loader import render_to_string
from django.views.generic import View
import common
from helpers import environs_helper, clusters_helper


class EnvCapacityConfigView(View):
    def get(self, request, name, stage):
        if request.is_ajax():
            # return data for ajax calls
            hosts = environs_helper.get_env_capacity(request, name, stage, capacity_type="HOST")
            groups = environs_helper.get_env_capacity(request, name, stage, capacity_type="GROUP")
            basic_cluster_info = clusters_helper.get_cluster(request, name, stage)
            if basic_cluster_info:
                cluster_name = '{}-{}'.format(name, stage)
                groups.remove(cluster_name)

            env = environs_helper.get_env_by_stage(request, name, stage)
            html = render_to_string("configs/capacity.tmpl", {
                "env": env,
                "hosts": ','.join(hosts),
                "groups": ','.join(groups),
                "csrf_token": get_token(request),

            })
            return HttpResponse(json.dumps({'html': html}), content_type="application/json")

        # otherwise, return a page
        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        hosts = environs_helper.get_env_capacity(request, name, stage, capacity_type="HOST")
        groups = environs_helper.get_env_capacity(request, name, stage, capacity_type="GROUP")
        basic_cluster_info = clusters_helper.get_cluster(request, name, stage)
        if basic_cluster_info:
            cluster_name = '{}-{}'.format(name, stage)
            groups.remove(cluster_name)

        return render(request, 'configs/capacity.html', {
            "env": env,
            "stages": stages,
            "hosts": ','.join(hosts),
            "groups": ','.join(groups),
        })

    def post(self, request, name, stage):
        query_dict = request.POST
        hosts_str = query_dict["hosts"]
        hosts = []
        if hosts_str:
            hosts = [x.strip() for x in hosts_str.split(',')]
        environs_helper.update_env_capacity(request, name, stage, capacity_type="HOST", data=hosts)

        groups_str = query_dict["groups"]
        groups = []
        if groups_str:
            groups = [x.strip() for x in groups_str.split(',')]

        basic_cluster_info = clusters_helper.get_cluster(request, name, stage)
        if basic_cluster_info:
            cluster_name = '{}-{}'.format(name, stage)
            groups.append(cluster_name)
        environs_helper.update_env_capacity(request, name, stage, capacity_type="GROUP",
                                            data=groups)

        return self.get(request, name, stage)

''' TODO figure out how to update detach and attach later
def update_capacity(request, name, stage):
    query_dict = request.POST
    groups_str = query_dict["updatedGroups"]
    hosts_str = query_dict["updatedHosts"]
    canary_hosts_str = None
    attached_hosts_str = None
    if "canaryHosts" in query_dict:
        canary_hosts_str = query_dict["canaryHosts"]
    if "attachedHosts" in query_dict:
        attached_hosts_str = query_dict["attachedHosts"]

    groups = []
    if groups_str:
        groups = [x.strip() for x in groups_str.split(',')]

    hosts = []
    if hosts_str:
        hosts = [x.strip() for x in hosts_str.split(',')]

    canary_hosts = []
    if canary_hosts_str:
        canary_hosts = [x.strip() for x in canary_hosts_str.split(',')]

    attached_hosts = []
    if attached_hosts_str:
        attached_hosts = [x.strip() for x in attached_hosts_str.split(',')]

    # compare groups with original groups
    orig_groups = client.getEnvGroups(name, stage)
    removed_groups = []
    for group in orig_groups:
        if group not in groups:
            # need to remove this host
            removed_groups.append(group)
        else:
            groups.remove(group)
    if removed_groups:
        client.removeGroupsFromEnv(name, stage, removed_groups, request.teletraan_user_id)

    # these are the new groups
    client.addGroupsToEnv(name, stage, groups, request.teletraan_user_id)

    # compare hosts with original hosts
    orig_hosts = client.getEnvHosts(name, stage)
    removed_hosts = []
    for host in orig_hosts:
        if host not in hosts:
            removed_hosts.append(host)
        else:
            hosts.remove(host)
    if removed_hosts:
        client.removeHostsFromEnv(name, stage, removed_hosts, request.teletraan_user_id)

    # there are new hosts
    client.addHostsToEnv(name, stage, hosts, request.teletraan_user_id)

    # detach hosts from ASG
    for host_name in canary_hosts:
        hostInfo = client.getHostInfos(host_name)
        if len(hostInfo) > 0:
            group_names = client.getASGNamesByHostId(hostInfo[0].hostId)
            for group_name in group_names:
                hostId = []
                hostId.append(hostInfo[0].hostId)
                try:
                    client.detachInstancesFromAutoScalingGroup(hostId, group_name)
                except:
                    log.error(traceback.format_exc())
                    raise

    # attach host to ASG
    for host_name in attached_hosts:
        hostInfo = client.getHostInfos(host_name)
        if len(hostInfo) > 0:
            group_names = client.getASGNamesByHostId(hostInfo[0].hostId)
            for group_name in group_names:
                hostId = []
                hostId.append(hostInfo[0].hostId)
                try:
                    client.attachInstancesToAutoScalingGroup(hostId, group_name)
                except:
                    log.error(traceback.format_exc())
                    raise

    # TODO set a confirmation first, and ask for confirmation if
    # delete or shutdown services are needed
    return redirect('/env/{}/{}/config/'.format(name, stage))


'''

