# Copyright 2016 Pinterest, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# -*- coding: utf-8 -*-

from deploy_board.webapp.helpers.deployclient import DeployClient

deploy_client = DeployClient()


def get_hosts(request, env_name, stage_name):
    return deploy_client.get(
        "/envs/%s/%s/hosts" % (env_name, stage_name), request.teletraan_user_id.token
    )


def get_host_by_env_and_hostname(request, env_name, stage_name, host_name):
    return deploy_client.get(
        "/envs/%s/%s/hosts/%s" % (env_name, stage_name, host_name),
        request.teletraan_user_id.token,
    )


def stop_service_on_host(request, env_name, stage_name, host_ids, replace_host):
    return deploy_client.delete(
        "/envs/%s/%s/hosts?replaceHost=%s" % (env_name, stage_name, replace_host),
        request.teletraan_user_id.token,
        data=host_ids,
    )


def get_host_tags(request, env_name, stage_name, tag_name):
    return deploy_client.get(
        "/envs/%s/%s/host_tags/%s" % (env_name, stage_name, tag_name),
        request.teletraan_user_id.token,
    )


def remove_host_tags(request, env_name, stage_name, tag_name):
    return deploy_client.delete(
        "/envs/%s/%s/host_tags/%s" % (env_name, stage_name, tag_name),
        request.teletraan_user_id.token,
    )


def get_ec2_host_tags(request, env_name, stage_name):
    return deploy_client.get(
        "/envs/%s/%s/host_tags?ec2Tags=true" % (env_name, stage_name),
        request.teletraan_user_id.token,
    )


def create_deploy_constraint(request, env_name, stage_name, data):
    return deploy_client.post(
        "/envs/%s/%s/deploy_constraint" % (env_name, stage_name),
        request.teletraan_user_id.token,
        data=data,
    )


def remove_deploy_constraint(request, env_name, stage_name):
    return deploy_client.delete(
        "/envs/%s/%s/deploy_constraint" % (env_name, stage_name),
        request.teletraan_user_id.token,
    )


def update_deploy_constraint(request, env_name, stage_name, data):
    return deploy_client.post(
        "/envs/%s/%s/deploy_constraint" % (env_name, stage_name),
        request.teletraan_user_id.token,
        data=data,
    )


def get_deploy_constraint(request, env_name, stage_name):
    return deploy_client.get(
        "/envs/%s/%s/deploy_constraint" % (env_name, stage_name),
        request.teletraan_user_id.token,
    )
