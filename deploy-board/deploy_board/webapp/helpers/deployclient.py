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
"""Helper class to connect Teletraan service
"""
import logging
from .decorators import singleton
from deploy_board.settings import (
    TELETRAAN_SERVICE_URL,
    TELETRAAN_SERVICE_VERSION,
    TELETRAAN_SERVICE_PROXY_HTTP,
    TELETRAAN_SERVICE_PROXY_HTTPS,
    TELETRAAN_SERVICE_USE_BEARER,
)
from .base_client import BaseClient


log = logging.getLogger(__name__)


@singleton
class DeployClient(BaseClient):
    def __init__(self):
        BaseClient.__init__(
            self,
            url_prefix=TELETRAAN_SERVICE_URL,
            version=TELETRAAN_SERVICE_VERSION,
            proxy_http=TELETRAAN_SERVICE_PROXY_HTTP,
            proxy_https=TELETRAAN_SERVICE_PROXY_HTTPS,
            bearer=TELETRAAN_SERVICE_USE_BEARER,
        )
