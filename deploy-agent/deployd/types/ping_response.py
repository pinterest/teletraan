from deployd.types.deploy_goal import DeployGoal
from deployd.types.opcode import OpCode


class PingResponse(object):
    def __init__(self, jsonValue=None):
        self.opCode = OpCode.NOOP
        self.deployGoal = None

        if jsonValue:
            # TODO: Only used for migration, should remove later
            if isinstance(jsonValue.get('opCode'), int):
                self.opCode = OpCode._VALUES_TO_NAMES[jsonValue.get('opCode')]
            else:
                self.opCode = jsonValue.get('opCode')

            if jsonValue.get('deployGoal'):
                self.deployGoal = DeployGoal(jsonValue=jsonValue.get('deployGoal'))

    def __str__(self):
        return "PingResponse(opCode={}, deployGoal={})".format(self.opCode, self.deployGoal)
