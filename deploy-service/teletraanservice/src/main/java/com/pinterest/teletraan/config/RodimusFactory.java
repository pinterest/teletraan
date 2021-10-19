package com.pinterest.teletraan.config;


import com.fasterxml.jackson.annotation.JsonProperty;

public class RodimusFactory {
    @JsonProperty
    private String rodimusUrl;

    @JsonProperty
    private String knoxKey;

    public String getRodimusUrl() {
        return this.rodimusUrl;
    }

    public void setRodimusUrl(String rodimusUrl) {
        this.rodimusUrl = rodimusUrl;
    }

    public String getKnoxKey() {
        return this.knoxKey;
    }

    public void setKnoxKey(String keyName) {
        this.knoxKey = keyName;
    }
}
