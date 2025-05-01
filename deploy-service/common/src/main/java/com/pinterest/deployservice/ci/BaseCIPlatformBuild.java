package com.pinterest.deployservice.ci;

public abstract class BaseCIPlatformBuild implements CIPlatformBuild {
    private String buildID;
    private String status;
    private long startTimestamp;
    private long duration;

    public BaseCIPlatformBuild(String buildID, String status, long startTimestamp, long duration) {
        this.buildID = buildID;
        this.status = status;
        this.startTimestamp = startTimestamp;
        this.duration = duration;
    }

    @Override
    public String getBuildUUID() {
        return this.buildID;
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }

    public long getDuration() {
        return this.duration;
    }

}
