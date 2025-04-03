package com.pinterest.deployservice.ci;

public abstract class BaseCIPlatformManager implements CIPlatformManager {
    private String typeName;
    private int priority;

    public BaseCIPlatformManager(String typeName, int priority) {
        this.typeName = typeName;
        this.priority = priority;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String startBuild(String pipelineName, String buildParams) throws Exception {
        return "";
    }

    @Override
    public Object getBuild(String pipelineName, String buildId) throws Exception {
        return null;
    }

    @Override
    public boolean jobExist(String jobName) throws Exception {
        return false;
    }
    
    @Override
    public Object getBuildObject() throws Exception {
        return null;
    }
}
