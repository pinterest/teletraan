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
"""Some common functions"""

import logging
from .helpers import environs_helper, deploys_helper, builds_helper, tags_helper

DEFAULT_BUILD_SIZE = 30
DEFAULT_COMMITS_SIZE = 30
BUILD_STAGE = "BUILD"
DEFAULT_STAGE_TYPE = "DEFAULT"

logger = logging.getLogger(__name__)


class UserIdentity(object):
    def __init__(self, name=None, token=None):
        self.name = name
        self.token = token


def get_all_stages(envs, stage):
    stages = []
    env = None
    for temp in envs:
        if stage and stage == temp["stageName"]:
            env = temp
        stages.append(temp["stageName"])
    stages.sort()
    if not env:
        stage = stages[0]
        for temp in envs:
            if temp["stageName"] == stage:
                env = temp
                break
    return stages, env


def is_agent_failed(agent):
    return (agent["status"] != "SUCCEEDED" and agent["status"] != "UNKNOWN") or agent[
        "state"
    ] == "PAUSED_BY_SYSTEM"


# Return the last completed deploy
def get_last_completed_deploy(request, env):
    # the first one is the current deploy
    result = deploys_helper.get_all(request, envId=[env["id"]], pageIndex=1, pageSize=2)
    deploys = result.get("deploys")
    if not deploys or len(deploys) < 2:
        logging.info(
            "Could not find last completed deploy in env %s/%s"
            % (env["envName"], env["stageName"])
        )
        return None
    else:
        return deploys[1]


# Return the last completed deploy
def get_previous_deploy(request, env, deploy):
    result = deploys_helper.get_all(
        request, envId=[env["id"]], before=deploy["startDate"], pageIndex=1, pageSize=2
    )
    deploys = result.get("deploys")
    if not deploys or len(deploys) < 2:
        logging.info("Could not find a deploy before deploy %s" % deploy["id"])
        return None
    else:
        return deploys[1]


# Return all the commits, up to allowed size, also a
# boolean indicated if the result was truncated
# Notice it will never return endSha itself
def get_commits_batch(
    request, scm, repo, startSha, endSha, size=DEFAULT_COMMITS_SIZE, keep_first=False
):
    commits = builds_helper.get_commits(
        request=request, scm=scm, repo=repo, startSha=startSha, endSha=endSha, size=size
    )
    truncated = False
    new_start_sha = None
    if commits and len(commits) >= size:
        new_start_sha = commits[-1]["sha"]
        truncated = True

    if not keep_first and commits:
        commits.pop(0)

    return commits, truncated, new_start_sha


# it will return all the commits, or max
def get_commits_between(request, scm, repo, startSha, endSha, max=500):
    total_commits = []
    keep_first = True
    new_start_sha = startSha

    while True:
        if len(total_commits) >= max:
            logging.error(
                "Exceeded max allowed commits, repo=%s, startSha=%s, endSha=%s"
                % (repo, startSha, endSha)
            )
            break

        commits, truncated, new_start_sha = get_commits_batch(
            request, scm, repo, new_start_sha, endSha, keep_first=keep_first
        )
        keep_first = False
        total_commits.extend(commits)
        if not truncated:
            break

    return total_commits


def deploy(request, name, stage):
    query_dict = request.POST
    desc = query_dict.get("description", None)
    buildId = query_dict["buildId"]
    return deploys_helper.deploy(request, name, stage, buildId, description=desc)


def restart(request, name, stage):
    return deploys_helper.restart(request, name, stage)


def rollback_to(request, name, stage, deploy_id):
    query_dict = request.POST
    desc = query_dict.get("description", None)
    mark_build_as_bad = (
        True if query_dict.get("mark_build_as_bad", "on") == "on" else False
    )
    buildId = query_dict.get("toBeMarkedBuildId", None)
    if mark_build_as_bad and buildId:
        tag = {
            "targetId": buildId,
            "targetType": "Build",
            "value": tags_helper.TagValue.BAD_BUILD,
            "comments": "deploy rollback, mark build as bad." + desc,
        }
        logger.info(
            "env {} stage {} rollback, mark buildId {} as {}".format(
                name, stage, buildId, tag
            )
        )
        builds_helper.set_build_tag(request, tag)
    return deploys_helper.rollback(request, name, stage, deploy_id, description=desc)


def promote(request, name, stage):
    query_dict = request.POST
    toStage = query_dict["toStage"]
    desc = query_dict.get("description", None)
    env = environs_helper.get_env_by_stage(request, name, stage)
    return deploys_helper.promote(
        request, name, toStage, env["deployId"], description=desc
    )


def clone_from_stage_name(
    request,
    env_name,
    stage_name,
    from_env_name,
    from_stage_name,
    stage_type,
    description,
    external_id,
    project_name,
):
    from_stage = environs_helper.get_env_by_stage(
        request, from_env_name, from_stage_name
    )
    agent_configs = environs_helper.get_env_agent_config(
        request, from_env_name, from_stage_name
    )
    script_configs = environs_helper.get_env_script_config(
        request, from_env_name, from_stage_name
    )
    alarms_configs = environs_helper.get_env_alarms_config(
        request, from_env_name, from_stage_name
    )
    metrics_configs = environs_helper.get_env_metrics_config(
        request, from_env_name, from_stage_name
    )
    webhooks_configs = environs_helper.get_env_hooks_config(
        request, from_env_name, from_stage_name
    )
    promotes_configs = environs_helper.get_env_promotes_config(
        request, from_env_name, from_stage_name
    )

    new_data = {}
    new_data["envName"] = env_name
    new_data["stageName"] = stage_name
    new_data["description"] = description
    new_data["buildName"] = from_stage["buildName"]
    new_data["branch"] = from_stage["branch"]
    new_data["chatroom"] = from_stage["chatroom"]
    new_data["maxParallel"] = from_stage["maxParallel"]
    new_data["priority"] = from_stage["priority"]
    new_data["stuckThreshold"] = from_stage["stuckThreshold"]
    new_data["successThreshold"] = from_stage["successThreshold"]
    new_data["acceptanceType"] = from_stage["acceptanceType"]
    new_data["emailRecipients"] = from_stage["emailRecipients"]
    new_data["notifyAuthors"] = from_stage["notifyAuthors"]
    new_data["watchRecipients"] = from_stage["watchRecipients"]
    new_data["groupMentionRecipients"] = from_stage["groupMentionRecipients"]
    new_data["maxDeployNum"] = from_stage["maxDeployNum"]
    new_data["maxDeployDay"] = from_stage["maxDeployDay"]
    new_data["overridePolicy"] = from_stage["overridePolicy"]
    new_data["stageType"] = stage_type
    new_data["externalId"] = external_id
    new_data["projectName"] = project_name

    new_stage = environs_helper.create_env(request, new_data) # ava 1.0.0.0.0

    # now clone all the extra configs
    if agent_configs:
        environs_helper.update_env_agent_config(
            request, env_name, stage_name, agent_configs
        )
    if script_configs:
        environs_helper.update_env_script_config(
            request, env_name, stage_name, script_configs
        )
    if alarms_configs:
        environs_helper.update_env_alarms_config(
            request, env_name, stage_name, alarms_configs
        )
    if metrics_configs:
        environs_helper.update_env_metrics_config(
            request, env_name, stage_name, metrics_configs
        )
    if webhooks_configs:
        environs_helper.update_env_hooks_config(
            request, env_name, stage_name, webhooks_configs
        )
    if promotes_configs:
        environs_helper.update_env_promotes_config(
            request, env_name, stage_name, promotes_configs
        )

    return new_stage


def create_simple_stage( # ava 1.0.0.2.0
    request, env_name, stage_name, stage_type, description, external_id, project_name
):
    """Create a new stage that does not require cloning an existing stage. Here, "simple" means that it does not require cloning."""
    data = {}
    data["envName"] = env_name
    data["stageName"] = stage_name
    data["description"] = description
    data["externalId"] = external_id
    data["stageType"] = stage_type
    data["projectName"] = project_name
    return environs_helper.create_env(request, data)


def get_cluster_name(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)
    return env.get("clusterName")


def get_env_groups(request, name, stage):
    groups = environs_helper.get_env_capacity(
        request, name, stage, capacity_type="GROUP"
    )
    return groups
