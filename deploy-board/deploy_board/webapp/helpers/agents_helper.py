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
"""Collection of all agents related calls
"""
from deploy_board.webapp.helpers.deployclient import DeployClient

deployclient = DeployClient()


def get_agents(request, env_name, stage_name):
    return deployclient.get("/envs/%s/%s/agents" % (env_name, stage_name),
                            request.teletraan_user_id.token)


def reset_failed_agents(request, env_name, stage_name, deploy_id):
    return deployclient.put("/envs/%s/%s/agents/reset_failed_agents/%s" % (env_name, stage_name,
                                                                           deploy_id),
                            request.teletraan_user_id.token)


def get_agent_error(request, env_name, stage_name, host_name):
    return deployclient.get("/envs/%s/%s/agents/errors/%s" % (env_name, stage_name, host_name),
                            request.teletraan_user_id.token)['errorMessage']


def retry_deploy(request, env_name, stage_name, host_id):
    return deployclient.put("/envs/%s/%s/agents/%s" % (env_name, stage_name, host_id),
                            request.teletraan_user_id.token, data={"state": "RESET"})

def reset_all_environments(request, host_id):
    return deployclient.put(
        "/agents/id/%s" % (host_id,),
        request.teletraan_user_id.token,
        data={"state": "RESET"},
    )

def pause_deploy(request, env_name, stage_name, host_id):
    return deployclient.put("/envs/%s/%s/agents/%s" % (env_name, stage_name, host_id),
                            request.teletraan_user_id.token, data={"state": "PAUSED_BY_USER"})


def resume_deploy(request, env_name, stage_name, host_id):
    return deployclient.put("/envs/%s/%s/agents/%s" % (env_name, stage_name, host_id),
                            request.teletraan_user_id.token, data={"state": "NORMAL"})


def get_agents_by_host(request, host_name):
    return deployclient.get("/agents/%s" % host_name, request.teletraan_user_id.token)


def get_agents_total_by_env(request, env_id):
    return deployclient.get("/agents/env/%s/total" % env_id, request.teletraan_user_id.token)

def get_agent_ec2_tags(request, env_name, stage_name):
    return deployclient.get(f"/env/{env_name}/{stage_name}/host_ec2_tags", request.teletraan_user_id.token)

