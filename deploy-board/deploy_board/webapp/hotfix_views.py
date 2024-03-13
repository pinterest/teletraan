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
"""Collection of all hotfix related views
"""
from django.shortcuts import render, redirect
from django.template.loader import render_to_string
from django.views.generic import View
from django.http import HttpResponse
from deploy_board.settings import BUILD_URL
from .helpers import environs_helper, deploys_helper, hotfixs_helper, builds_helper, systems_helper
from . import common

DEFAULT_PAGE_SIZE = 30


def _caculateLink(urlPattern, repo, commit):
    return urlPattern % (repo, commit)


def _create_commits(commits, urlPattern, hotfix):
    for commit in hotfix['commits'].split(','):
        fixed_commit = commit.strip()
        commit = {}
        commit["id"] = fixed_commit
        commit["link"] = _caculateLink(urlPattern, hotfix['repo'], fixed_commit)
        commits.append(commit)


def get_jenkins_url(hotfix):
    jenkins_url = "%s/%s" % (BUILD_URL, hotfix['jobName'])
    if hotfix['jobNum']:
        jenkins_url = "%s/%s/%s" % (BUILD_URL, hotfix['jobName'], hotfix['jobNum'])
    return jenkins_url


def get_hotfix(request, name, stage, id):
    env = environs_helper.get_env_by_stage(request, name, stage)
    hotfix = hotfixs_helper.get(request, id)
    deploy = deploys_helper.get(request, hotfix['baseDeployId'])
    build = builds_helper.get_build(request, deploy['buildId'])
    urlPattern = systems_helper.get_url_pattern(request, build.get('type'))
    commits = []
    _create_commits(commits, urlPattern['template'], hotfix)
    jenkins_url = get_jenkins_url(hotfix)
    return render(request, 'hotfixs/hotfix_detail.html', {
        "env": env,
        "hotfix": hotfix,
        "commits": commits,
        "deploy": deploy,
        "build": build,
        "jenkins_url": jenkins_url
    })


def get_hotfix_detail(request, id):
    hotfix = hotfixs_helper.get(request, id)
    deploy = deploys_helper.get(request, hotfix['baseDeployId'])
    build = builds_helper.get_build(request, deploy['buildId'])
    urlPattern = systems_helper.get_url_pattern(request, build.get('type'))
    commits = []
    _create_commits(commits, urlPattern['template'], hotfix)
    jenkins_url = get_jenkins_url(hotfix)
    html = render_to_string('hotfixs/hotfix_detail.tmpl', {
        "hotfix": hotfix,
        "commits": commits,
        "deploy": deploy,
        "build": build,
        "jenkins_url": jenkins_url
    })
    return HttpResponse(html)


def patch(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)
    deploy_id = request.GET.get('base_deploy', None)
    if not deploy_id:
        deploy_id = env['deployId']
    deploy = deploys_helper.get(request, deploy_id)
    build = builds_helper.get_build(request, deploy['buildId'])
    commits, truncated, new_start_sha = common.get_commits_batch(request, build['type'], 
                                                                 build['repo'],
                                                                 env['branch'],
                                                                 build['commit'],
                                                                 keep_first=True)
    return render(request, 'hotfixs/cherry_pick.html', {
        "deploy": deploy,
        "build": build,
        "env": env,
        "commits": commits,
        "start_sha": new_start_sha,
        "end_sha": build['commit'],
        "repo": build['repo'],
        "scm": build['type'],
        "truncated": truncated,
    })


class HotfixesView(View):
    def get(self, request, name, stage):
        env = environs_helper.get_env_by_stage(request, name, stage)
        index = int(request.GET.get('page_index', '1'))
        size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
        hotfixes = hotfixs_helper.get_all(request, name, index, size)
        build = builds_helper.get_build(request, deploy['buildId'])
        urlPattern = systems_helper.get_url_pattern(request, build.get('type'))
        for hotfix in hotfixes:
            commits = []
            _create_commits(commits, urlPattern['template'], hotfix)
            hotfix["commits"] = commits

        return render(request, 'hotfixs/hotfix_landing.html', {
            "env": env,
            "hotfixes": hotfixes,
            "pageIndex": index,
            "pageSize": DEFAULT_PAGE_SIZE,
            "disablePrevious": index <= 1,
            "disableNext": len(hotfixes) < DEFAULT_PAGE_SIZE,
        })

    def post(self, request, name, stage):
        query_dict = request.POST
        baseDeployId = query_dict["baseDeployId"]
        commits_txt = query_dict["commits"]
        commits = commits_txt.split(",")
        # It is very important to reverse the commits ordering since we want to apply
        # the older commit first,
        # TODO reevaluate this, we really should have the backend do this accurately
        commits.reverse()
        # now, we have to re-generate the string again, cause the backend expect string
        # TODO need a better way to handle this
        commits_txt = ','.join(commits)
        id = hotfixs_helper.create(request, name, baseDeployId, commits_txt)['id']
        return redirect('/env/%s/%s/hotfix/%s' % (name, stage, id))
