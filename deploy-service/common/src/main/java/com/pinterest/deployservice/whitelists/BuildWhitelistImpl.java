package com.pinterest.deployservice.whitelists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BuildWhitelistImpl implements Whitelist {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BuildWhitelistImpl.class);

    private List<String> validBuildURLs;
    private List<String> trustedBuildURLs;

    public BuildWhitelistImpl(List<String> whitelist, List<String> trustedlist) {
        this.validBuildURLs = whitelist;
        this.trustedBuildURLs = trustedlist;
    }

    // approved checks if build matches approved URL whitelist
    public Boolean approved(String buildName) {
        for (String pattern : validBuildURLs) {
            if (buildName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    // trusted checks if build matches trusted URL whitelist
    public Boolean trusted(String buildName) {
        for (String pattern : trustedBuildURLs) {
            if (buildName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }
}