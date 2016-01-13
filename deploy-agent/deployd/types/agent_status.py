class AgentStatus(object):
    # Deploy step successfully completed
    SUCCEEDED = 0
    # Deploy step is in unknown status, mostly set in the beginning of the step
    UNKNOWN = 1
    # Deploy agent error, unexpected and not retryable, server pause agent install
    AGENT_FAILED = 2
    # Deploy agent error, retryable,
    # server should allow agent to retry, until reached max retry times
    RETRYABLE_AGENT_FAILED = 3
    # TODO This is not currently used at all, consider to remove it
    SCRIPT_FAILED = 4
    # TODO Agent execution is aborted manually on the host, server does not handle it right now
    ABORTED_BY_SERVICE = 5
    # Service script execution timed out, server pause agent install
    SCRIPT_TIMEOUT = 6
    # Too many retries executing service script, server pause agent install
    TOO_MANY_RETRY = 7
    # Runtime version verification mismatch, server pause agent install
    RUNTIME_MISMATCH = 8
    # Use for deploy-agent internally
    ABORTED_BY_SERVER = 9

    _VALUES_TO_NAMES = {
        0: "SUCCEEDED",
        1: "UNKNOWN",
        2: "AGENT_FAILED",
        3: "RETRYABLE_AGENT_FAILED",
        4: "SCRIPT_FAILED",
        5: "ABORTED_BY_SERVICE",
        6: "SCRIPT_TIMEOUT",
        7: "TOO_MANY_RETRY",
        8: "RUNTIME_MISMATCH",
        9: "ABORTED_BY_SERVER",
    }

    _NAMES_TO_VALUES = {
        "SUCCEEDED": 0,
        "UNKNOWN": 1,
        "AGENT_FAILED": 2,
        "RETRYABLE_AGENT_FAILED": 3,
        "SCRIPT_FAILED": 4,
        "ABORTED_BY_SERVICE": 5,
        "SCRIPT_TIMEOUT": 6,
        "TOO_MANY_RETRY": 7,
        "RUNTIME_MISMATCH": 8,
        "ABORTED_BY_SERVER": 9,
    }
