package com.pinterest.teletraan.config;


import com.fasterxml.jackson.annotation.JsonProperty;

public class RodimusFactory {
    @JsonProperty
    private String rodimusUrl;

    @JsonProperty
    private String knoxKey;

    @JsonProperty
    private boolean useProxy;

    @JsonProperty
    private String httpProxyAddr;

    @JsonProperty
    private String httpProxyPort;

    public boolean getUseProxy() {
        return useProxy;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public String getHttpProxyAddr() {
        return httpProxyAddr;
    }

    public void setHttpProxyAddr(String httpProxyAddr) {
        this.httpProxyAddr = httpProxyAddr;
    }

    public String getHttpProxyPort() {
        return httpProxyPort;
    }

    public void setHttpProxyPort(String httpProxyPort) {
        this.httpProxyPort = httpProxyPort;
    }

    public String getRodimusUrl() {
        return rodimusUrl;
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
