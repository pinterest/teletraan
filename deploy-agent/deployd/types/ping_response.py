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

import json

from deployd.types.deploy_goal import DeployGoal
from deployd.types.multi_goal_response_item import MultiGoalResponseItem
from deployd.common.types import OpCode


class PingResponse(object):
    def __init__(self, jsonValue=None) -> None:
        self.opCode = OpCode.NOOP
        self.deployGoal = None
        self.multiGoalResponse = None

        if jsonValue:
            self.opCode = jsonValue.get("opCode")

            if jsonValue.get("deployGoal"):
                self.deployGoal = DeployGoal(jsonValue=jsonValue.get("deployGoal"))

            if jsonValue.get("multiGoalResponse"):
                self.multiGoalResponse = []
                for jsonItem in jsonValue.get("multiGoalResponse"):
                    self.multiGoalResponse.append(
                        MultiGoalResponseItem(jsonValue=jsonItem)
                    )

    def to_dict(self) -> str:
        json_dict = self.__dict__.copy()

        # Ensure deployGoal field is json serializable
        deployGoal = json_dict.get("deployGoal")
        if isinstance(deployGoal, DeployGoal):
            json_dict["deployGoal"] = deployGoal.to_dict()

        # Ensure multiGoalResponse field is json serializable
        multiGoalResponse = json_dict.get("multiGoalResponse")
        if isinstance(multiGoalResponse, list):
            json_dict["multiGoalResponse"] = []
            for multiGoalResponseItem in multiGoalResponse:
                if isinstance(multiGoalResponseItem, MultiGoalResponseItem):
                    multiGoalResponseItem = multiGoalResponseItem.to_dict()
                json_dict["multiGoalResponse"].append(multiGoalResponseItem)
        return json_dict

    def __str__(self) -> str:
        return json.dumps(self.to_dict(), indent=2)
