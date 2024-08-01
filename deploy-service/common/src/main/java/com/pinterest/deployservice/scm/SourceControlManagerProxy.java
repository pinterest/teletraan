package com.pinterest.deployservice.scm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pinterest.deployservice.bean.CommitBean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceControlManagerProxy {
    private static final Logger LOG = LoggerFactory.getLogger(SourceControlManagerProxy.class);
    private List<String> validSCMs;

    Map<String, SourceControlManager> managers;
    String defaultScmTypeName;

    public SourceControlManagerProxy(Map<String, SourceControlManager> managers, String defaultScmTypeName) {
        this.managers = managers;
        this.defaultScmTypeName = defaultScmTypeName;
        validSCMs = new ArrayList<String> (this.managers.keySet());
    }

    private SourceControlManager getSourceControlManager(String scmType) throws Exception {
        if(StringUtils.isEmpty(scmType)) {
            scmType = defaultScmTypeName;
        }
        SourceControlManager manager = this.managers.get(scmType);
        if (manager == null) {
            LOG.error("Unsupported SCM type: " + scmType);
            throw new Exception("Unsupported SCM type: " + scmType);
        }
        return manager;
    }

    public List<String> getSCMs() throws Exception {
        return validSCMs;
    }

    public Boolean hasSCMType(String scmName) {
        return validSCMs.stream().anyMatch(scmName::equalsIgnoreCase);
    }

    public String getDefaultTypeName() {
        return defaultScmTypeName;
    }

    public String getCommitLinkTemplate(String scmType) throws Exception {
        return getSourceControlManager(scmType).getCommitLinkTemplate();
    }

    public String getUrlPrefix(String scmType) throws Exception {
        return getSourceControlManager(scmType).getUrlPrefix();
    }

    public String generateCommitLink(String scmType, String repo, String sha) throws Exception {
        return getSourceControlManager(scmType).generateCommitLink(repo, sha);
    }

    public CommitBean getCommit(String scmType, String repo, String sha) throws Exception {
        return getSourceControlManager(scmType).getCommit(repo, sha);
    }

    public List<CommitBean> getCommits(String scmType, String repo, String startSha, String endSha, int size) throws Exception {
        return getSourceControlManager(scmType).getCommits(repo, startSha, endSha, size);
    }

    public List<CommitBean> getCommits(String scmType, String repo, String startSha, String endSha, int size, String path) throws Exception {
        return getSourceControlManager(scmType).getCommits(repo, startSha, endSha, size, path);
    }
}
