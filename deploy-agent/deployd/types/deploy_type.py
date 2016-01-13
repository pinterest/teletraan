class DeployType(object):
    # Regular deploy
    REGULAR = 0
    # Special deploy which should go fast
    HOTFIX = 1
    # Special deploy to redeploy certain previous build
    ROLLBACK = 2
    # Special deploy to redeploy current build
    RESTART = 3
    # Special deploy to stop service
    STOP = 4

    _VALUES_TO_NAMES = {
        0: "REGULAR",
        1: "HOTFIX",
        2: "ROLLBACK",
        3: "RESTART",
        4: "STOP",
    }

    _NAMES_TO_VALUES = {
        "REGULAR": 0,
        "HOTFIX": 1,
        "ROLLBACK": 2,
        "RESTART": 3,
        "STOP": 4,
    }
