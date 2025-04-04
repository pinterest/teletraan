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
"""Collection of build related calls"""

from deploy_board.webapp.helpers.deployclient import DeployClient

deploy_client = DeployClient()


def get_build(request, id):
    return deploy_client.get("/builds/%s" % id, request.teletraan_user_id.token)


def get_branches(request, name):
    return deploy_client.get(
        "/builds/names/%s/branches" % name, request.teletraan_user_id.token
    )


def get_builds(request, **kwargs):
    params = deploy_client.gen_params(kwargs)
    return deploy_client.get("/builds/", request.teletraan_user_id.token, params=params)


def publish_build(request, build):
    return deploy_client.post("/builds/", request.teletraan_user_id.token, data=build)


def get_build_names(request, **kwargs):
    params = deploy_client.gen_params(kwargs)
    return deploy_client.get(
        "/builds/names/", request.teletraan_user_id.token, params=params
    )


def delete_build(request, id):
    return deploy_client.delete("/builds/%s" % id, request.teletraan_user_id.token)


def get_commits(request, **kwargs):
    params = deploy_client.gen_params(kwargs)
    return deploy_client.get(
        "/commits/", request.teletraan_user_id.token, params=params
    )


def get_commit(request, repo, sha):
    return deploy_client.get(
        "/commits/%s/%s" % (repo, sha), request.teletraan_user_id.token
    )


def get_builds_and_tags(request, **kwargs):
    params = deploy_client.gen_params(kwargs)
    return deploy_client.get(
        "/builds/tags", request.teletraan_user_id.token, params=params
    )


def get_build_and_tag(request, id):
    return deploy_client.get(
        "/builds/{0}/tags".format(id), request.teletraan_user_id.token
    )


def set_build_tag(request, tag):
    return deploy_client.post("/tags/", request.teletraan_user_id.token, data=tag)


def del_build_tag(request, id):
    return deploy_client.delete(
        "/tags/{0}/".format(id), request.teletraan_user_id.token
    )
