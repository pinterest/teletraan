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

import os

# Pinterest specific settings
IS_PINTEREST = True if os.getenv("IS_PINTEREST", "false") == "true" else False
METRIC_PORT_HEALTH = int(os.getenv('METRIC_PORT_HEALTH')) if os.getenv('METRIC_PORT_HEALTH', False) else None
METRIC_CACHE_PATH = os.getenv('METRIC_CACHE_PATH', None)
TELEFIG_BINARY = os.getenv('TELEFIG_BINARY', None)
MAIN_LOGGER = "deployd"
STATSBOARD_URL=os.getenv('STATSBOARD_URL', "https://statsboard.pinadmin.com/api/v1/")
REDEPLOY_MAX_RETRY = 3

# 0: puppet applied successfully with no changes
# 2: puppet applied successfully with changes
PUPPET_SUCCESS_EXIT_CODES = [0, 2]

__version__ = '1.2.60'