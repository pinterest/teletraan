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

from deployd.types.deploy_goal import DeployGoal
from deployd.types.opcode import OperationCode
from deployd.common.types import OpCode


class PingResponse(object):
    def __init__(self, jsonValue=None) -> None:
        self.opCode = OpCode.NOOP
        self.deployGoal = None
        self._jsonValue = None

        if jsonValue:
            self._jsonValue = jsonValue
            # TODO: Only used for migration, should remove later
            if isinstance(jsonValue.get("opCode"), int):
                self.opCode = OperationCode._VALUES_TO_NAMES[jsonValue.get("opCode")]
            else:
                self.opCode = jsonValue.get("opCode")

            if jsonValue.get("deployGoal"):
                self.deployGoal = DeployGoal(jsonValue=jsonValue.get("deployGoal"))

    def __str__(self) -> str:
        return "PingResponse(opCode={}, deployGoal={})".format(
            self.opCode, self.deployGoal
        )
