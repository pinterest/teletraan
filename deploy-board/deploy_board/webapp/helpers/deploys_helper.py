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

from deploy_board.webapp.helpers.deployclient import DeployClient

deployclient = DeployClient()


def get(request, id):
    return deployclient.get("/deploys/%s" % id, request.teletraan_user_id.token)


def get_all(request, **kwargs):
    params = deployclient.gen_params(kwargs)
    return deployclient.get("/deploys", request.teletraan_user_id.token, params=params)


def deploy(request, env_name, stage_name, build_id, description=None):
    params = [("buildId", build_id)]
    if description:
        params.append(("description", description))
    return deployclient.post(
        "/envs/%s/%s/deploys" % (env_name, stage_name),
        request.teletraan_user_id.token,
        params=params,
    )


def pause(request, env_name, stage_name):
    params = [("actionType", "PAUSE")]
    return deployclient.post(
        "/envs/%s/%s/deploys/current/actions" % (env_name, stage_name),
        request.teletraan_user_id.token,
        params=params,
    )


def resume(request, env_name, stage_name):
    params = [("actionType", "RESUME")]
    return deployclient.post(
        "/envs/%s/%s/deploys/current/actions" % (env_name, stage_name),
        request.teletraan_user_id.token,
        params=params,
    )


def restart(request, env_name, stage_name):
    params = [("actionType", "RESTART")]
    return deployclient.post(
        "/envs/%s/%s/deploys/current/actions" % (env_name, stage_name),
        request.teletraan_user_id.token,
        params=params,
    )


def rollback(request, env_name, stage_name, to_deploy_id, description=None):
    params = [("actionType", "ROLLBACK"), ("toDeployId", to_deploy_id)]
    if description:
        params.append(("description", description))
    return deployclient.post(
        "/envs/%s/%s/deploys/current/actions" % (env_name, stage_name),
        request.teletraan_user_id.token,
        params=params,
    )


def promote(request, env_name, stage_name, from_deploy_id, description=None):
    params = [("actionType", "PROMOTE"), ("fromDeployId", from_deploy_id)]
    if description:
        params.append(("description", description))
    return deployclient.post(
        "/envs/%s/%s/deploys/current/actions" % (env_name, stage_name),
        request.teletraan_user_id.token,
        params=params,
    )


def delete(request, id):
    return deployclient.delete("/deploys/%s" % id, request.teletraan_user_id.token)


def get_current(request, env_name, stage_name):
    return deployclient.get(
        "/envs/%s/%s/deploys/current" % (env_name, stage_name),
        request.teletraan_user_id.token,
    )


def update_progress(request, env_name, stage_name):
    return deployclient.put(
        "/envs/%s/%s/deploys/current/progress" % (env_name, stage_name),
        request.teletraan_user_id.token,
    )


def update(request, id, data):
    return deployclient.put(
        "/deploys/%s" % id, request.teletraan_user_id.token, data=data
    )


def get_missing_hosts(request, env_name, stage_name):
    return deployclient.get(
        "/envs/%s/%s/deploys/current/missing-hosts" % (env_name, stage_name),
        request.teletraan_user_id.token,
    )


def get_daily_deploy_count(request):
    return deployclient.get("/deploys/dailycount", request.teletraan_user_id.token)
