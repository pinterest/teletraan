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
"""Collection of all admin related calls"""

from deploy_board.webapp.helpers.deployclient import DeployClient

deployclient = DeployClient()


def get_env_users(request, env_name, user_types):
    return deployclient.get(
        "/envs/%s/%s" % (env_name, user_types), request.teletraan_user_id.token
    )


def get_env_user(request, env_name, user_name, user_types):
    return deployclient.get(
        "/envs/%s/%s/%s" % (env_name, user_types, user_name),
        request.teletraan_user_id.token,
    )


def delete_env_user(request, env_name, user_name, user_types):
    return deployclient.delete(
        "/envs/%s/%s/%s" % (env_name, user_types, user_name),
        request.teletraan_user_id.token,
    )


def create_env_user(request, env_name, user_name, role, user_types):
    user = {"name": user_name, "role": role}
    return deployclient.post(
        "/envs/%s/%s" % (env_name, user_types),
        request.teletraan_user_id.token,
        data=user,
    )


def update_env_user(request, env_name, user_name, role, user_types):
    user = {"role": role}
    return deployclient.put(
        "/envs/%s/%s/%s" % (env_name, user_types, user_name),
        request.teletraan_user_id.token,
        data=user,
    )
