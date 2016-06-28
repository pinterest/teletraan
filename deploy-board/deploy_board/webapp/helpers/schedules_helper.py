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
"""Collection of all environs related calls
"""
from deploy_board.webapp.helpers.deployclient import DeployClient

def get_schedule(request, id):
    return deployclient.get("/schedules/%s" % (id), request.teletraan_user_id.token)

#request by schedule id or environment/stage id??  

def update_schedule(request, envName, stageName, cooldownTimes, hostNumbers):
	params =[("cooldownTimes", cooldownTimes), ("hostNumbers", hostNumbers)]

	if request.method == 'POST':
		return deployclient.post("/schedules/update/%s/%s" % (envName, stageName), request.teletraan_user_id.token,
							params=params)
	elif request.method == 'PUT': 
		return deployclient.put("/schedules/update/%s/%s" % (envName, stageName), request.teletraan_user_id.token,
							params=params)

def delete_schedule(request, envId, cooldownTimes, hostNumbers):
	return deployclient.delete("/schedules/update/%s/%s" % (envName, stageName), request.teletraan_user_id.token