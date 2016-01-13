from deployd.types.build import Build
from deployd.types.deploy_stage import DeployStage


class DeployGoal(object):
    def __init__(self, jsonValue=None):
        self.deployId = None
        self.envId = None
        self.envName = None
        self.stageName = None
        self.deployStage = None
        self.build = None
        self.deployAlias = None
        self.config = None
        self.scriptVariables = None
        self.firstDeploy = None

        if jsonValue:
            self.deployId = jsonValue.get('deployId')
            self.envId = jsonValue.get('envId')
            self.envName = jsonValue.get('envName')
            self.stageName = jsonValue.get('stageName')
            # TODO: Only used for migration, should remove later
            if isinstance(jsonValue.get('deployStage'), int):
                self.deployStage = DeployStage._VALUES_TO_NAMES[jsonValue.get('deployStage')]
            else:
                self.deployStage = jsonValue.get('deployStage')

            if jsonValue.get('build'):
                self.build = Build(jsonValue=jsonValue.get('build'))

            self.deployAlias = jsonValue.get('deployAlias')
            self.config = jsonValue.get('agentConfigs')
            self.scriptVariables = jsonValue.get('scriptVariables')
            self.firstDeploy = jsonValue.get('firstDeploy')

    def __str__(self):
        return "DeployGoal(deployId={}, envId={}, envName={}, stageName={}, " \
               "deployStage={}, build={}, deployAlias={}, agentConfig={}," \
               "scriptVariables={}, firstDeploy={})".format(self.deployId, self.envId, self.envName,
                                                            self.stageName, self.deployStage,
                                                            self.build, self.deployAlias,
                                                            self.config, self.scriptVariables,
                                                            self.firstDeploy)
