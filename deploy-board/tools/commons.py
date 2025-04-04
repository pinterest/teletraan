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

import os
import string
from urllib3 import request
import random
import time

from deploy_board.webapp.helpers import (
    environs_helper,
    builds_helper,
    systems_helper,
    deploys_helper,
    agents_helper,
)
from deploy_board.webapp.common import UserIdentity

token = os.environ.get("TELETRAAN_TEST_TOKEN")
if token:
    USER = UserIdentity(name="tester", token=token)
else:
    USER = UserIdentity(name="anonymous")
REQUEST = request
REQUEST.teletraan_user_id = USER


def gen_random_num(size=8, chars=string.digits):
    return "".join(random.choice(chars) for _ in range(size))


def create_env(name: str, stage: str):
    request = {}
    request["envName"] = name
    request["stageName"] = stage
    request["stageType"] = "PRODUCTION"
    env = environs_helper.create_env(REQUEST, request)
    print(f"Successfully created env {env['id']}")
    return env


def publish_build(build_name, branch="master", commit=gen_random_num(32)):
    request = {}
    request["name"] = build_name
    request["repo"] = "sample-repo"
    request["branch"] = branch
    request["commit"] = commit
    request["commitDate"] = int(round(time.time() * 1000))
    request["artifactUrl"] = "https://sample.com"
    request["publishInfo"] = "https://sample.com"
    build = builds_helper.publish_build(REQUEST, request)
    print("Successfully published build %s." % build["id"])
    return build


def get_environ_helper():
    return environs_helper


def get_deploy_helper():
    return deploys_helper


def get_build_helper():
    return builds_helper


def get_system_helper():
    return systems_helper


def get_agent_helper():
    return agents_helper
