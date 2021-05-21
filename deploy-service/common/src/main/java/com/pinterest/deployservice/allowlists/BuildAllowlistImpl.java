package com.pinterest.deployservice.allowlists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BuildAllowlistImpl implements Allowlist {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BuildAllowlistImpl.class);

    private List<String> validBuildURLs;
    private List<String> trustedBuildURLs;
    private List<String> soxBuildURLs;

    public BuildAllowlistImpl(List<String> allowlist, List<String> trustedlist, List<String> soxlist) {
        this.validBuildURLs = allowlist;
        this.trustedBuildURLs = trustedlist;
        this.soxBuildURLs = soxlist;
    }

    // approved checks if build matches approved URL allow list
    public Boolean approved(String buildName) {
        for (String pattern : validBuildURLs) {
            if (buildName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    // trusted checks if build matches trusted URL allow list
    public Boolean trusted(String buildName) {
        for (String pattern : trustedBuildURLs) {
            if (buildName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    // sox_compliant checks if build matches trusted URL allow list
    public Boolean sox_compliant(String buildName) {
        for (String pattern : soxBuildURLs) {
            if (buildName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }
}