package com.pinterest.deployservice.whitelists;

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