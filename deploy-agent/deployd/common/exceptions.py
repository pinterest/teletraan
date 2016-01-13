class DeployException(Exception):
    pass


class DeployTimeoutException(DeployException):
    pass


class DeployDownloadException(DeployException):
    pass


class DeployConfigException(DeployException):
    pass


class AgentException(DeployException):
    pass
