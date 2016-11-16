package com.pinterest.teletraan.config;


import com.fasterxml.jackson.annotation.JsonProperty;

public class RodimusFactory {
    @JsonProperty
    private String rodimusUrl;

    @JsonProperty
    private String token;

    public String getRodimusUrl() {
        return rodimusUrl;
    }

    public void setRodimusUrl(String rodimusUrl) {
        this.rodimusUrl = rodimusUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
