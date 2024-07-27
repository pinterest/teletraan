package com.pinterest.deployservice.events;

import com.google.common.collect.ImmutableMap;
import com.pinterest.teletraan.universal.events.ResourceChangedEvent;

public class DeployEvent extends ResourceChangedEvent {
    private final String env;

    private final String stage;

    private final String commit;

    private final String operator;

    public DeployEvent(Object source, String env, String stage, String commit, String operator) {
        super("deployed_build", operator, source, ImmutableMap.of("env", env, "stage", stage));
        this.operator = operator;
        this.env = env;
        this.stage = stage;
        this.commit = commit;
    }

    public String getEnv() {
        return env;
    }

    public String getStage() {
        return stage;
    }

    public String getCommit() {
        return commit;
    }

    public String getOperator() {
        return operator;
    }
}
