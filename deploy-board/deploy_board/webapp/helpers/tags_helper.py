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

from deploy_board.webapp.helpers.deployclient import DeployClient

deploy_client = DeployClient()

#In sync with deploy-service/common/src/main/java/com/pinterest/deployservice/bean/TagValue.java
class TagValue(object):
    BAD_BUILD="BAD_BUILD"
    GOOD_BUILD="GOOD_BUILD"
    CERTIFIED_BUILD="CERTIFIED_BUILD"
    CERTIFYING_BUILD="CERTIFYING_BUILD"

    
def get_latest_by_target_id(request, target_id):
    return deploy_client.get(
        "/tags/targets/%s/latest" % target_id, request.teletraan_user_id.token
    )
