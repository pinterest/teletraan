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
"""Collection of builds related views
"""
import json
from django.shortcuts import render, redirect
from django.http import HttpResponse
from django.template.loader import render_to_string
from . import common
from .helpers import builds_helper, systems_helper, tags_helper, deploys_helper
import random

import logging

log = logging.getLogger(__name__)

def builds_landing(request):
    return get_build_names(request)


def get_build_names(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', common.DEFAULT_BUILD_SIZE))
    filter = request.GET.get('filter', None)
    build_names = builds_helper.get_build_names(request, start=index, size=size, filter=filter)
    return render(request, 'builds/build_names.html', {
        'build_names': build_names,
        "pageIndex": index,
        "pageSize": size,
        "disablePrevious": index <= 1,
        "disableNext": len(build_names) < size,
        "filter": filter,
    })


def get_build(request, id):
    info = builds_helper.get_build_and_tag(request, id)
    tag = info.get("tag")
    if tag:
        tag["build"]=json.loads(tag["metaInfo"])
    return render(request, 'builds/build_details.html', {
        "build": info["build"],
        "tag": tag
    })


def list_builds(request, name):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', common.DEFAULT_BUILD_SIZE))
    builds = builds_helper.get_builds_and_tags(request, name=name, pageIndex=index, pageSize=size)
    return render(request, 'builds/builds.html', {
        'build_name': name,
        'builds': builds,
        "pageIndex": index,
        "pageSize": size,
        "disablePrevious": index <= 1,
        "disableNext": len(builds) < size,
    })


def get_all_builds(request):
    name = request.GET.get('name')
    branch = request.GET.get('branch')
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', common.DEFAULT_BUILD_SIZE))
    builds = builds_helper.get_builds_and_tags(request, name=name, branch=branch, pageIndex=index,
                                      pageSize=size)
    deploy_state = None
    current_build_id = request.GET.get('current_build_id', None)
    override_policy = request.GET.get('override_policy')
    deploy_id = request.GET.get('deploy_id')
    current_build = None
    scmType = ""
    if current_build_id:
        current_build = builds_helper.get_build_and_tag(request, current_build_id)
        current_build = current_build.get('build')
        scmType = current_build.get('type')
    if deploy_id:
        deploy_config = deploys_helper.get(request, deploy_id)
        if deploy_config:
            deploy_state = deploy_config.get('state', None)

    scm_url = systems_helper.get_scm_url(request, scmType)

    html = render_to_string('builds/pick_a_build.tmpl', {
        "builds": builds,
        "current_build": current_build,
        "scm_url": scm_url,
        "buildName": name,
        "branch": branch,
        "pageIndex": index,
        "pageSize": size,
        "disablePrevious": index <= 1,
        "disableNext": len(builds) < size,
        "overridePolicy": override_policy,
        "deployState": deploy_state,
    })
    return HttpResponse(html)


# currently we only support search by git commit or SHA, 7 letters or longer
def search_commit(request, commit):
    builds = builds_helper.get_builds_and_tags(request, commit=commit)
    return render(request, 'builds/builds_by_commit.html', {
        'commit': commit,
        'builds': builds,
    })


def list_build_branches(request, name):
    branches = builds_helper.get_branches(request, name=name)
    return HttpResponse(json.dumps(branches), content_type="application/json")


def get_more_commits(request):
    startSha = request.GET.get('start_sha')
    endSha = request.GET.get('end_sha')
    repo = request.GET.get('repo')
    scm = request.GET.get('scm')

    commits, truncated, new_start_sha = common.get_commits_batch(request, scm, repo,
                                                                 startSha, endSha,
                                                                 keep_first=False)

    show_checkbox_str = request.GET.get('show_checkbox', 'False')
    show_checkbox = show_checkbox_str.lower() == 'true'
    pagination_id = random.randint(0, 1000000)

    rows = render_to_string('builds/commit_rows.tmpl', {
        "commits": commits,
        "show_checkbox": show_checkbox,
        "pagination_id": pagination_id
    })
    return HttpResponse(json.dumps({'rows': rows, 'new_start_sha': new_start_sha,
                                    'truncated': truncated}),
                        content_type="application/json")

def compare_commits(request):
    startSha = request.GET.get('start_sha')
    endSha = request.GET.get('end_sha')
    repo = request.GET.get('repo')
    scm = request.GET.get('scm')
    commits, truncated, new_start_sha = common.get_commits_batch(request, scm, repo,
                                                                 startSha, endSha,
                                                                 keep_first=True)
    html = render_to_string('builds/commits.tmpl', {
        "commits": commits,
        "start_sha": new_start_sha,
        "end_sha": endSha,
        "repo": repo,
        "scm": scm,
        "truncated": truncated,
        "show_checkbox": False,
    })

    return HttpResponse(html)

def compare_commits_datatables(request):
    startSha = request.GET.get('start_sha')
    endSha = request.GET.get('end_sha')
    repo = request.GET.get('repo')
    scm = request.GET.get('scm')
    commits, truncated, new_start_sha = common.get_commits_batch(request, scm, repo,
                                                                 startSha, endSha,
                                                                 size=2000,
                                                                 keep_first=True)
    html = render_to_string('builds/show_commits.tmpl', {
        "commits": commits,
        "start_sha": new_start_sha,
        "end_sha": endSha,
        "repo": repo,
        "scm": scm,
        "truncated": truncated,
        "show_checkbox": False,
    })

    return HttpResponse(html)


def tag_build(request, id):
    if request.method == "POST":
        build_info = builds_helper.get_build_and_tag(request, id)
        current_tag = build_info.get("tag")

        if current_tag:
            tagged_build = json.loads(current_tag["metaInfo"])
            if tagged_build["id"] == id:
                log.info("There is already a tag associated with the build. Remove it")
                builds_helper.del_build_tag(request, current_tag["id"])
        tag = {
            "targetId": id,
            "targetType": "Build", 
            "comments": request.POST["comments"]
        }
        value = request.POST["tag_value"]
        if value.lower() == "good":
            tag["value"] = tags_helper.TagValue.GOOD_BUILD
        elif value.lower() == "bad":
            tag["value"] = tags_helper.TagValue.BAD_BUILD
        elif value.lower() == "certified":
            tag["value"] = tags_helper.TagValue.CERTIFIED_BUILD
        elif value.lower() == "certifying":
            tag["value"] = tags_helper.TagValue.CERTIFYING_BUILD
        else:
            return HttpResponse(status=400)
        builds_helper.set_build_tag(request, tag)
        return redirect("/builds/{0}/".format(id))
    else:
        return HttpResponse(status=405)
