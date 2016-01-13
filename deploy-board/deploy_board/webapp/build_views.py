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
from django.shortcuts import render
from django.http import HttpResponse
from django.template.loader import render_to_string
import common
from helpers import builds_helper, systems_helper
import random


def builds_landing(request):
    return get_build_names(request)


def get_build_names(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', common.DEFAULT_BUILD_SIZE))
    build_names = builds_helper.get_build_names(request, start=index, size=size)
    return render(request, 'builds/build_names.html', {
        'build_names': build_names,
        "pageIndex": index,
        "pageSize": common.DEFAULT_BUILD_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(build_names) < common.DEFAULT_BUILD_SIZE,
    })


def get_build(request, id):
    build = builds_helper.get_build(request, id)
    return render(request, 'builds/build_details.html', {
        "build": build,
    })


def list_builds(request, name):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', common.DEFAULT_BUILD_SIZE))
    builds = builds_helper.get_builds(request, name=name, pageIndex=index, pageSize=size)
    return render(request, 'builds/builds.html', {
        'build_name': name,
        'builds': builds,
        "pageIndex": index,
        "pageSize": common.DEFAULT_BUILD_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(builds) < common.DEFAULT_BUILD_SIZE,
    })


def get_all_builds(request):
    name = request.GET.get('name')
    branch = request.GET.get('branch')
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', common.DEFAULT_BUILD_SIZE))
    builds = builds_helper.get_builds(request, name=name, branch=branch, pageIndex=index,
                                      pageSize=size)
    scm_url = systems_helper.get_scm_url(request)
    current_build_id = request.GET.get('current_build_id', None)
    current_build = None
    if current_build_id:
        current_build = builds_helper.get_build(request, current_build_id)

    html = render_to_string('builds/pick_a_build.tmpl', {
        "builds": builds,
        "current_build": current_build,
        "scm_url": scm_url,
        "buildName": name,
        "branch": branch,
        "pageIndex": index,
        "pageSize": common.DEFAULT_BUILD_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(builds) < common.DEFAULT_BUILD_SIZE,
    })
    return HttpResponse(html)


# currently we only support search by git commit or SHA, 7 letters or longer
def search_commit(request, commit):
    builds = builds_helper.get_builds(request, commit=commit)
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

    commits, truncated, new_start_sha = common.get_commits_batch(request, repo,
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
    commits, truncated, new_start_sha = common.get_commits_batch(request, repo,
                                                                 startSha, endSha,
                                                                 keep_first=True)
    html = render_to_string('builds/commits.tmpl', {
        "commits": commits,
        "start_sha": new_start_sha,
        "end_sha": endSha,
        "repo": repo,
        "truncated": truncated,
        "show_checkbox": False,
    })

    return HttpResponse(html)

def compare_commits_datatables(request):
    startSha = request.GET.get('start_sha')
    endSha = request.GET.get('end_sha')
    repo = request.GET.get('repo')
    commits, truncated, new_start_sha = common.get_commits_batch(request, repo,
                                                                 startSha, endSha,
                                                                 size=2000,
                                                                 keep_first=True)
    html = render_to_string('builds/show_commits.tmpl', {
        "commits": commits,
        "start_sha": new_start_sha,
        "end_sha": endSha,
        "repo": repo,
        "truncated": truncated,
        "show_checkbox": False,
    })

    return HttpResponse(html)
