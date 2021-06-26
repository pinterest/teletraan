package com.pinterest.teletraan.config;


import com.fasterxml.jackson.annotation.JsonProperty;

public class RodimusFactory {
    @JsonProperty
    private String rodimusUrl;

    @JsonProperty
    private String role;

    public String getRodimusUrl() {
        return rodimusUrl;
    }

    public void setRodimusUrl(String rodimusUrl) {
        this.rodimusUrl = rodimusUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
