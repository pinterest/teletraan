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
"""Collection of all ngapp2 related calls
"""
import logging

from ngapptools.ext.zk import ZK, get_root
from ngapptools.varnish.disable_ngapp_canary import update_canary_data_for_version

logger = logging.getLogger(__name__)


class NgappDeployStep(object):
    ROLLBACK = -1
    SERVING_BUILD = 0
    PRE_DEPLOY = 1
    DEPLOY_TO_CANARY = 2
    DEPLOY_TO_PROD = 3
    POST_DEPLOY = 4


class Ngapp2DeployUtils(object):
    def __init__(self):
        self._zk = ZK()
        self._config = self._zk.get_config("ngapp2")

    def get_ok_to_serve(self):
        return self._zk.get_ok_to_serve(self._config)

    def get_previous_build(self):
        return self._zk.get_build(config=self._config, previous=True)

    def deploy_to_canary(self, build):
        self._zk.set_build(config=self._config, build=build, canary=True)
        self.set_status_to_zk("DEPLOY_TO_CANARY")

    def deploy_to_prod(self, build):
        # first disable canary traffic
        update_canary_data_for_version()
        self._zk.set_build(config=self._config, build=build)
        self.set_status_to_zk("DEPLOY_TO_PROD")

    def rollback_canary(self):
        # disable canary traffic
        update_canary_data_for_version()

    def rollback_prod(self):
        self._zk.swap_enabled_previous(self._config)

    def set_status_to_zk(self, stage):
        self._zk._safe_set("{}/ngapp2/deploy_status/stage".format(get_root()), stage)

    def get_status_from_zk(self):
        return self._zk.safe_get("{}/ngapp2/deploy_status/stage".format(get_root()),
                                 "serving_build")

    def set_deploying_env_to_zk(self, envName):
        self._zk._safe_set("{}/ngapp2/deploy_status/deploying_env".format(get_root()),
                           envName)

    def get_deploying_env_from_zk(self):
        return self._zk.safe_get("{}/ngapp2/deploy_status/deploying_env".format(get_root()))

    def reset_finish_message_flag(self, stage):
        self._zk._safe_set(self._get_message_sent_path(stage), False)

    def set_finish_message_flag(self, stage):
        self._zk._safe_set(self._get_message_sent_path(stage), True)

    def get_finish_message_flag(self, stage):
        return self._zk.safe_get(self._get_message_sent_path(stage), "False")

    def _get_message_sent_path(self, stage):
        return "{}/ngapp2/deploy_status/{}_message_sent".format(get_root(), stage)
