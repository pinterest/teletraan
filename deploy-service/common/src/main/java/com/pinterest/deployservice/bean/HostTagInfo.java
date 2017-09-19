package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;


public class HostTagInfo {
    @JsonProperty("hostName")
    private String hostName;

    @JsonProperty("hostId")
    private String hostId;

    @JsonProperty("tagValue")
    private String tagValue;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
