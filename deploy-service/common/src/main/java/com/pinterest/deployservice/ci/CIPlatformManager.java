package com.pinterest.deployservice.ci;

public interface CIPlatformManager {

    String getTypeName();

    int getPriority();

    String startBuild(String pipelineName, String buildParams) throws Exception;

    Object getBuild(String pipelineName, String buildId) throws Exception;

    boolean jobExist(String jobName) throws Exception;

    Object getBuildObject() throws Exception;
    
}
