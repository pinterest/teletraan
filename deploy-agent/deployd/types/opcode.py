class OpCode(object):
    # No action needed
    NOOP = 0
    # Agent need to install new build
    DEPLOY = 1
    # Agent needs to update status file based on the new deploy goal
    UPDATE = 2
    # Agent needs to restart service
    RESTART = 3
    # Agent needs to delete its own status file for this env
    DELETE = 4
    # Host is removed from env, agent needs to delete its status file and
    # shutdown service as well
    TERMINATE = 5
    # Agent needs to sleep for certain time
    WAIT = 6
    # Agent needs to rollback
    ROLLBACK = 7
    # Agent needs to shutdown service
    STOP = 8

    _VALUES_TO_NAMES = {
        0: "NOOP",
        1: "DEPLOY",
        2: "UPDATE",
        3: "RESTART",
        4: "DELETE",
        5: "TERMINATE",
        6: "WAIT",
        7: "ROLLBACK",
        8: "STOP",
    }

    _NAMES_TO_VALUES = {
        "NOOP": 0,
        "DEPLOY": 1,
        "UPDATE": 2,
        "RESTART": 3,
        "DELETE": 4,
        "TERMINATE": 5,
        "WAIT": 6,
        "ROLLBACK": 7,
        "STOP": 8,
    }
