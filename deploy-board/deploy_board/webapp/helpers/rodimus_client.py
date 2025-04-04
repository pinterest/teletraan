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
"""Helper class to connect Rodimus service"""

import logging
from .decorators import singleton
from deploy_board.settings import (
    RODIMUS_SERVICE_URL,
    RODIMUS_SERVICE_VERSION,
    RODIMUS_SERVICE_PROXY_HTTP,
    RODIMUS_SERVICE_PROXY_HTTPS,
    RODIMUS_SERVICE_USE_BEARER,
)
from .base_client import BaseClient

log = logging.getLogger(__name__)


@singleton
class RodimusClient(BaseClient):
    def __init__(self):
        BaseClient.__init__(
            self,
            url_prefix=RODIMUS_SERVICE_URL,
            version=RODIMUS_SERVICE_VERSION,
            proxy_http=RODIMUS_SERVICE_PROXY_HTTP,
            proxy_https=RODIMUS_SERVICE_PROXY_HTTPS,
            bearer=RODIMUS_SERVICE_USE_BEARER,
        )
