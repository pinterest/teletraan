package com.pinterest.deployservice.scm;

import java.util.HashMap;
import java.util.List;

import com.pinterest.deployservice.bean.CommitBean;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceControlManagerProxy {
    private static final Logger LOG = LoggerFactory.getLogger(SourceControlManagerProxy.class);
    private final static String DEFAULT_TYPE = PhabricatorManager.TYPE;

    HashMap<String, SourceControlManager> managers;

    public SourceControlManagerProxy(HashMap<String, SourceControlManager> managers) {
        this.managers = managers;
    }

    private SourceControlManager getSourceControlManager(String scmType) throws Exception {
        if(StringUtils.isEmpty(scmType)) {
            scmType = DEFAULT_TYPE;
        }
        SourceControlManager manager = this.managers.get(scmType);
        if (manager == null) {
            LOG.error("Unsupported SCM type: " + scmType);
            throw new Exception("Unsupported SCM type: " + scmType);
        }
        return manager;
    }

    public String getType() {
        return DEFAULT_TYPE;
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
