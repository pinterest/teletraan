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
"""Collection of all ngapp2 related views
"""
from django.shortcuts import render, redirect
from django.views.generic import View
from helpers import environs_helper, deploys_helper, builds_helper, systems_helper
from deploy_board import settings
import subprocess
import common
import logging
import os
import requests
import traceback

from ngapptools.varnish.varnish_info import Recorder
from helpers.ngapp2_deploy import Ngapp2DeployUtils, NgappDeployStep
from helpers import groups_helper

logger = logging.getLogger(__name__)

__author__ = 'jihe'

SLACK_CHANNEL = settings.NGAPP_DEPLOY_CHANNEL

NGAPP_A = "ngapp2-A"
NGAPP_B = "ngapp2-B"
NGAPP_GROUP = "webapp"
DEFAULT_PAGE_SIZE = 30


def get_last_ngapp2_deploy(request, curr_env, stage):
    if curr_env == NGAPP_A:
        env = environs_helper.get_env_by_stage(request, NGAPP_B, stage)
    else:
        env = environs_helper.get_env_by_stage(request, NGAPP_A, stage)
    if env:
        return deploys_helper.get(request, env['deployId'])


def is_prod():
    return os.environ.get("ENV_STAGE") == "prod"


def get_slack_channel():
    if is_prod():
        return settings.NGAPP_DEPLOY_CHANNEL
    else:
        return "deploys_local"


def get_ngapp2_compare_deploy_url(start_build_id, end_build_id):
    return "https://deploy.pinadmin.com/ngapp2/compare_deploys/?chkbox_1=%s&chkbox_2=%s" % \
           (start_build_id, end_build_id)


def get_notify_authors_message(request, env, current_build):
    try:
        last_succ_deploy = get_last_ngapp2_deploy(request, env['envName'], env['stageName'])
        last_build = builds_helper.get_build(request, last_succ_deploy['buildId'])
        start_commit = current_build.get('commit')
        end_commit = last_build.get('commit')
        repo = last_build['repo']
        commits = common.get_commits_between(request, repo=repo, startSha=start_commit,
                                             endSha=end_commit)
        authors = set()
        for commit in commits:
            author = commit.get('author')
            if not author or author.lower() == "unknown":
                continue
            authors.add("<@{}>".format(author))
        mentions = ",".join(authors)
        return "This deploy features {} commits from the " \
               "following pingineers: {}. See changes <{}|here>".format(len(commits), mentions,
                                                                        get_ngapp2_compare_deploy_url(
                                                                            current_build.get("id"),
                                                                            last_build.get("id")))
    except:
        logger.error(traceback.format_exc())
        return ""


def execute(cmd):
    try:
        logger.info("Executing comand '%s'..." % cmd)
        logname = os.path.join(os.environ.get("LOG_DIR"), "ngapp_varnish_status.log")
        with open(logname, "a+") as f:
            pid = subprocess.Popen(cmd, stdout=f, stderr=subprocess.STDOUT,
                                   preexec_fn=os.setsid).pid
            return pid
    except:
        logger.warning(traceback.format_exc())
        return -1


def get_deploy_url(envName, stage):
    return "https://deploy.pinadmin.com/env/{}/{}/deploy/".format(envName, stage)


def sendStartMessage(request, user, envName, stageName, notifyAuthor):
    env = environs_helper.get_env_by_stage(request, envName, stageName)
    deploy = deploys_helper.get(request, env['deployId'])
    build = builds_helper.get_build(request, deploy['buildId'])
    branch = build['branch']
    Ngapp2DeployUtils().reset_finish_message_flag(stageName)
    message = "{}/{}: deploy of {}/{} started. See details <{}|here>.".format(
        envName, stageName, branch, build['commitShort'], get_deploy_url(envName, stageName))
    systems_helper.send_chat_message(request, user.name, get_slack_channel(), message)

    if not notifyAuthor:
        return

    author_msg = get_notify_authors_message(request, env, build)
    if author_msg and is_prod():
        systems_helper.send_chat_message(request, user.name, get_slack_channel(), author_msg)
    elif author_msg:
        logger.info("get author list message:{}".format(author_msg))


def sendFinishMessage(request, user, envName, stageName, state):
    ngapp2DeployUtils = Ngapp2DeployUtils()

    # if we've already sent the message
    if ngapp2DeployUtils.get_finish_message_flag(stageName) == "True":
        return
    env = environs_helper.get_env_by_stage(request, envName, stageName)
    deploy = deploys_helper.get(request, env['deployId'])
    build = builds_helper.get_build(request, deploy['buildId'])
    branch = build['branch']
    commit = build['commit']
    weblink = get_deploy_url(envName, stageName)
    state = state.lower()
    if state == "succeeding" or state == "done":
        template = "{}/{}: deploy of {}/{} completed successfully. See details <{}|here>"
    else:
        template = "{}/{}: deploy of {}/{} failed. See details <{}|here>"
    message = template.format(envName, stageName, branch, commit[:7], weblink)
    ngapp2DeployUtils.set_finish_message_flag(stageName)
    systems_helper.send_chat_message(request, deploy['operator'], get_slack_channel(), message)


def enable_health_check(request):
    groups_helper.enable_health_check(request, NGAPP_GROUP)


def disable_health_check(request):
    groups_helper.disable_health_check(request, NGAPP_GROUP)


def update_env_priority(request, env_name):
    if env_name == NGAPP_A:
        curr_env = NGAPP_A
        prev_env = NGAPP_B
    else:
        curr_env = NGAPP_B
        prev_env = NGAPP_A

    curr_data = {"priority": "HIGH"}
    prev_data = {"priority": "NORMAL"}
    environs_helper.update_env_basic_config(request, curr_env, "prod", data=curr_data)
    environs_helper.update_env_basic_config(request, prev_env, "prod", data=prev_data)


class NgappStatusView(View):
    ENDING_STATE = {NgappDeployStep.POST_DEPLOY, NgappDeployStep.ROLLBACK}

    def get(self, request):
        params = request.GET
        deploy_stage = getattr(NgappDeployStep, params.get("deploy_stage").upper())
        env_name = params.get("env_name")
        user = request.teletraan_user_id
        if deploy_stage == NgappDeployStep.PRE_DEPLOY:
            state, response = self.get_varnish_status(request, env_name, deploy_stage)
        elif deploy_stage == NgappDeployStep.DEPLOY_TO_CANARY:
            state, response = self.get_deploy_status(request, name=env_name, stage="canary")
            # sending message to the chatroom.
            if state == "failing" or state == "succeeding":
                sendFinishMessage(request, user, env_name, "canary", state)
        elif deploy_stage == NgappDeployStep.DEPLOY_TO_PROD:
            state, response = self.get_deploy_status(request, name=env_name, stage="prod")
            # if state is failing, sending failure message
            if state == "failing":
                sendFinishMessage(request, user, env_name, "prod", state)
        else:
            state, response = self.get_varnish_status(request, env_name, deploy_stage)
            if state == "DONE":
                sendFinishMessage(request, user, env_name, "prod", state)

                # set higher priority to current env
                update_env_priority(request, env_name)

                # enable health check
                enable_health_check(request)
        return response

    def get_varnish_status(self, request, env_name, deploy_stage):
        if deploy_stage == NgappDeployStep.PRE_DEPLOY:
            page = 'ngapp2/ngapp2_pre_deploy.html'
            status_file = settings.NGAPP_PRE_DEPLOY_STATUS_NODE
        elif deploy_stage == NgappDeployStep.POST_DEPLOY:
            page = 'ngapp2/ngapp2_post_deploy.html'
            status_file = settings.NGAPP_POST_DEPLOY_STATUS_NODE
        else:
            page = 'ngapp2/ngapp2_rollback.html'
            status_file = settings.NGAPP_ROLLBACK_STATUS_NODE

        recorder = Recorder(status_file)
        state, total, correct = recorder.read_varish_info()
        total = int(total)
        correct = int(correct)
        progress = correct * 100 / total
        if deploy_stage in self.ENDING_STATE and state == "DONE":
            Ngapp2DeployUtils().set_status_to_zk("SERVING_BUILD")

        return state, render(request, page, {
            "current_status": state,
            "correct_node": correct,
            "total_node": total,
            "progress": progress,
            "env_name": env_name
        })

    def get_deploy_status(self, request, name, stage):
        env = environs_helper.get_env_by_stage(request, name, stage)
        deploy = deploys_helper.get(request, env['deployId'])
        build = builds_helper.get_build(request, deploy['buildId'])
        canary_message = None
        if is_prod() and stage == "canary" and deploy['state'] == "SUCCEEDING":
            expected = build['commit']
            running_build = self.get_canary_version_internal()
            if expected[:7] == running_build[:7]:
                canary_message = \
                    "curl https://canary.pinterest.com. " \
                    "Canary host is running on the correct version: {}".format(expected[:7])
            else:
                canary_message = "curl https://canary.pinterest.com. Running version: {}, " \
                                 "Expect version: {}".format(running_build[:7], expected[:7])
                deploy['state'] = "RUNNING"

        if deploy['state'] == "SUCCEEDING" or deploy['state'] == "SUCCEEDED":
            state = "succeeding"
        elif deploy['state'] == "ABORTED" or deploy['state'] == "FAILING":
            state = "failing"
        else:
            state = "running"
        return state, render(request, 'ngapp2/ngapp2_deploy.tmpl', {
            "build": build,
            "env": env,
            "deploy": deploy,
            "canaryMessage": canary_message
        })

    def get_canary_version_internal(self):
        """Get running build version by looking up endpoint. This is mainly used
        for optimus project
        """
        end_point = "https://canary.pinterest.com"
        logger.info('Obtaining commit by query endpoint {}.'.format(end_point))
        try:
            response = requests.get(url=end_point)
            if response and response.headers.get('pinterest-version'):
                return response.headers['pinterest-version']
            else:
                logger.error('Failed to query endpoint {} '
                             'with response code: {}'.format(end_point, response.status_code))
                return None
        except requests.ConnectionError:
            logger.error(
                'Failed to query endpoint {} (Possibly because the service is not running).')
            return None
        except:
            logger.error(traceback.format_exc())
            return None


class EnvStatus(object):
    def __init__(self, envStage, deploy, commit):
        self.build = commit
        self.envName = envStage['envName']
        self.stageName = envStage['stageName']
        if deploy:
            self.deployType = deploy['type']
            self.startDate = deploy['startDate']
            self.operator = deploy['operator']
        else:
            self.deployType = "REGULAR"
            self.startDate = 0
            self.operator = "Unknown"


def deploy_to_canary(request, name):
    params = request.POST
    build = params.get('commit7', None)
    ngapp2_deploy_utils = Ngapp2DeployUtils()
    ngapp2_deploy_utils.deploy_to_canary(build)
    common.deploy(request, name, "canary")
    user = request.teletraan_user_id
    sendStartMessage(request, user, name, "canary", notifyAuthor=True)
    return redirect("/ngapp2/deploy/")


def promote_to_prod(request, deploy_id, build):
    params = request.POST
    description = params.get("description", None)
    ngapp2_deploy_utils = Ngapp2DeployUtils()
    ngapp2_deploy_utils.deploy_to_prod(build)
    user = request.teletraan_user_id
    env_name = ngapp2_deploy_utils.get_deploying_env_from_zk()
    deploys_helper.promote(request, env_name, "prod", deploy_id, description)
    sendStartMessage(request, user, env_name, "prod", notifyAuthor=False)
    return redirect("/ngapp2/deploy/")


def get_all_deploys(request):
    env_stage_a = environs_helper.get_env_by_stage(request, NGAPP_A, "prod")
    env_stage_b = environs_helper.get_env_by_stage(request, NGAPP_B, "prod")
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', '%d' % DEFAULT_PAGE_SIZE))
    filter = {}
    filter['envId'] = [env_stage_a['id'], env_stage_b['id']]
    filter['pageIndex'] = index
    filter['pageSize'] = size
    result = deploys_helper.get_all(request, **filter)
    deploy_summaries = []
    for deploy in result['deploys']:
        build = builds_helper.get_build(request, deploy['buildId'])
        summary = {}
        summary['deploy'] = deploy
        summary['build'] = build
        deploy_summaries.append(summary)

    return render(request, 'ngapp2/ngapp2_history.html', {
        "deploy_summaries": deploy_summaries,
        "pageIndex": index,
        "pageSize": DEFAULT_PAGE_SIZE,
        "disablePrevious": index <= 1,
        "disableNext": len(result['deploys']) < DEFAULT_PAGE_SIZE,
    })


def compare_deploys(request):
    configs = {}
    for key, value in request.GET.iteritems():
        if key.startswith('chkbox_'):
            index = key[len('chkbox_'):]
            configs[index] = value
    indexes = configs.keys()
    start_build_id = configs[indexes[0]]
    end_build_id = configs[indexes[1]]
    if int(indexes[0]) > int(indexes[1]):
        start_build_id = configs[indexes[1]]
        end_build_id = configs[indexes[0]]

    start_build = builds_helper.get_build(request, start_build_id)
    startSha = start_build['commit']
    repo = start_build['repo']
    end_build = builds_helper.get_build(request, end_build_id)
    endSha = end_build['commit']

    return render(request, 'ngapp2/ngapp2_deploy_commits.html', {
        "startSha": startSha,
        "endSha": endSha,
        "repo": repo,
    })


class NgappView(View):
    def get(self, request):
        ngapp2_deploy_utils = Ngapp2DeployUtils()
        ok_to_serve = ngapp2_deploy_utils.get_ok_to_serve()

        curr_build = ok_to_serve.get('default')
        ok_to_serve_version = ok_to_serve.get('versions')
        prev_build = ngapp2_deploy_utils.get_previous_build()

        env_stage_a = environs_helper.get_env_by_stage(request, NGAPP_A, "prod")
        env_stage_b = environs_helper.get_env_by_stage(request, NGAPP_B, "prod")
        deploy_a = None
        build_a = None
        deploy_b = None
        if env_stage_a.get('deployId'):
            deploy_a = deploys_helper.get(request, env_stage_a['deployId'])
            build_a = builds_helper.get_build(request, deploy_a['buildId'])
        if env_stage_b.get('deployId'):
            deploy_b = deploys_helper.get(request, env_stage_b['deployId'])

        stage = getattr(NgappDeployStep, ngapp2_deploy_utils.get_status_from_zk().upper())
        if stage != NgappDeployStep.SERVING_BUILD:
            deploying_env = ngapp2_deploy_utils.get_deploying_env_from_zk()
        else:
            if build_a and build_a['commit'][:7] == curr_build[:7]:
                deploying_env = env_stage_b['envName']
            else:
                deploying_env = env_stage_a['envName']

            ngapp2_deploy_utils.set_deploying_env_to_zk(deploying_env)

        if deploying_env == env_stage_a['envName']:
            curr_env = EnvStatus(env_stage_b, deploy_b, curr_build)
            prev_env = EnvStatus(env_stage_a, deploy_a, prev_build)
        else:
            curr_env = EnvStatus(env_stage_a, deploy_a, curr_build)
            prev_env = EnvStatus(env_stage_b, deploy_b, prev_build)

        return render(request, 'ngapp2/ngapp2_deploy.html', {
            "prev_env": prev_env,
            "curr_env": curr_env,
            "deploy_stage": stage,
            "deploying_env": deploying_env,
            "serve_version_count": len(ok_to_serve_version)
        })

    def post(self, request):
        try:
            params = request.POST
            action = params.get("action", "deploy")
            stage = params.get("current_stage")
            ngapp2_deploy_utils = Ngapp2DeployUtils()
            if action == "cancel":
                if stage == "deploy_to_canary":
                    ngapp2_deploy_utils.rollback_canary()
                elif stage == "deploy_to_prod":
                    ngapp2_deploy_utils.rollback_prod()

                ngapp2_deploy_utils.set_status_to_zk("SERVING_BUILD")
            else:
                if stage == 'pre_deploy':
                    self.start_pre_deploy(request)
                elif stage == 'post_deploy':
                    self.start_post_deploy(request)
                elif stage == 'rollback':
                    self.start_roll_back()

                ngapp2_deploy_utils.set_status_to_zk(stage)
        except:
            logger.error(traceback.format_exc())
            logger.warning(traceback.format_exc())
        finally:
            return redirect("/ngapp2/deploy/")

    def start_pre_deploy(self, request):
        Recorder(settings.NGAPP_PRE_DEPLOY_STATUS_NODE).init_info()
        if is_prod():
            virtual_env = os.path.join(os.environ.get("VIRTUAL_ENV"), "bin")
            cmd = [os.path.join(virtual_env, "python"),
                   os.path.join(virtual_env, "ngapp-pre-deploy")]
        else:
            cmd = [os.path.join(os.environ.get("BASE_DIR"), "integ_test/ngapp2/pre_deploy")]

        result = execute(cmd=cmd)
        if result < 0:
            raise
        # also disable health check for webapp
        disable_health_check(request)

    def start_post_deploy(self, request):
        Recorder(settings.NGAPP_POST_DEPLOY_STATUS_NODE).init_info()
        if is_prod():
            virtual_env = os.path.join(os.environ.get("VIRTUAL_ENV"), "bin")
            cmd = [os.path.join(virtual_env, "python"),
                   os.path.join(virtual_env, "ngapp-post-deploy")]
        else:
            cmd = [os.path.join(os.environ.get("BASE_DIR"), "integ_test/ngapp2/post_deploy")]
        result = execute(cmd=cmd)
        if result < 0:
            raise

    def start_roll_back(self):
        Recorder(settings.NGAPP_ROLLBACK_STATUS_NODE).init_info()
        if is_prod():
            virtual_env = os.path.join(os.environ.get("VIRTUAL_ENV"), "bin")
            cmd = [os.path.join(virtual_env, "python"), os.path.join(virtual_env, "ngapp-rollback")]
        else:
            cmd = [os.path.join(os.environ.get("BASE_DIR"), "integ_test/ngapp2/rollback")]
        result = execute(cmd)
        if result < 0:
            raise
