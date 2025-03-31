package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuildkiteFactory {
    @JsonProperty private String buildkitePortalBaseUrl;
    @JsonProperty private String buildkiteApiBaseUrl;

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
}
