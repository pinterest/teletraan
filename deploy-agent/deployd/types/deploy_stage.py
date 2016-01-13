class DeployStage(object):
    # Reserved by deploy system
    UNKNOWN = 0
    # (Optional) Unregister with LB/ZK, drain traffic, shut down service etc.
    PRE_DOWNLOAD = 1
    # Reserved by deploy service, agent download service binary
    DOWNLOADING = 2
    # (Optional) Unregister with LB/ZK, drain traffic, shut down service etc.
    POST_DOWNLOAD = 3
    # Reserved by deploy service, agent setup symbolic link to the latest package etc.
    STAGING = 4
    # (Optional) For service to do anything before restart with the downloaded binary
    PRE_RESTART = 5
    # (Optional) Service restart, or start
    RESTARTING = 6
    # (Optional) Testing, register back with LB/ZK, warmup, double checking and etc.
    POST_RESTART = 7
    # Reserved by deploy service, serving the expected build
    SERVING_BUILD = 8
    # Deploy Agent is shutting down the service
    STOPPING = 9
    # Complete shutting down the service
    STOPPED = 10

    _VALUES_TO_NAMES = {
        0: "UNKNOWN",
        1: "PRE_DOWNLOAD",
        2: "DOWNLOADING",
        3: "POST_DOWNLOAD",
        4: "STAGING",
        5: "PRE_RESTART",
        6: "RESTARTING",
        7: "POST_RESTART",
        8: "SERVING_BUILD",
        9: "STOPPING",
        10: "STOPPED",
    }

    _NAMES_TO_VALUES = {
        "UNKNOWN": 0,
        "PRE_DOWNLOAD": 1,
        "DOWNLOADING": 2,
        "POST_DOWNLOAD": 3,
        "STAGING": 4,
        "PRE_RESTART": 5,
        "RESTARTING": 6,
        "POST_RESTART": 7,
        "SERVING_BUILD": 8,
        "STOPPING": 9,
        "STOPPED": 10,
    }
