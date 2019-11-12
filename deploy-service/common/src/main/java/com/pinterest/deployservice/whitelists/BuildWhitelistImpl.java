package com.pinterest.deployservice.whitelists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BuildWhitelistImpl implements Whitelist {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BuildWhitelistImpl.class);

    private List<String> validBuildURLs;

    public BuildWhitelistImpl(List<String> whitelist) {
        this.validBuildURLs = whitelist;
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
}