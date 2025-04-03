package com.pinterest.teletraan.config;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.deployservice.ci.Buildkite;
import com.pinterest.deployservice.ci.CIPlatformManager;

@JsonTypeName("buildkite")
public class BuildkiteFactory implements CIPlatformFactory {
    @JsonProperty private String buildkitePortalBaseUrl;
    @JsonProperty private String buildkiteApiBaseUrl;
    @NotNull @JsonProperty private String typeName;
    @NotNull @JsonProperty private int priority;

    public String getBuildkitePortalBaseUrl() {
        return this.buildkitePortalBaseUrl;
    }

    public void setBuildkitePortalBaseUrl(String buildkitePortalBaseUrl) {
        this.buildkitePortalBaseUrl = buildkitePortalBaseUrl;
    }

    public String getBuildkiteApiBaseUrl() {
        return this.buildkiteApiBaseUrl;
    }
    public void setBuildkiteApiBaseUrl(String buildkiteApiBaseUrl) {
        this.buildkiteApiBaseUrl = buildkiteApiBaseUrl;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public CIPlatformManager create() throws Exception {
        return new Buildkite(
                buildkitePortalBaseUrl,
                buildkiteApiBaseUrl,
                typeName,
                priority);
    }
}
