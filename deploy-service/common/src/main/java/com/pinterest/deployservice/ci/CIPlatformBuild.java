package com.pinterest.deployservice.ci;

public interface CIPlatformBuild {
    String getBuildUUID();
    int getProgress();
    String getStatus();
}
