/**
 * Copyright (c) 2022-2024 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.scm;

import com.google.common.net.PercentEscaper;
import com.pinterest.deployservice.bean.CommitBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import javax.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceControlManagerProxy {
    private static final Logger LOG = LoggerFactory.getLogger(SourceControlManagerProxy.class);
    private static final PercentEscaper SCM_KEY_ESCAPER = new PercentEscaper("-._~", false);
    // Repository grants share the legacy role table's indexed resource_id column.
    private static final int MAX_REPOSITORY_RESOURCE_NAME_LENGTH = 175;
    private List<String> validSCMs;

    Map<String, SourceControlManager> managers;
    String defaultScmTypeName;

    public SourceControlManagerProxy(
            Map<String, SourceControlManager> managers, String defaultScmTypeName) {
        this.managers = managers;
        this.defaultScmTypeName = defaultScmTypeName;
        validSCMs = new ArrayList<String>(this.managers.keySet());
    }

    private SourceControlManager getSourceControlManager(String scmType) {
        if (StringUtils.isEmpty(scmType)) {
            scmType = defaultScmTypeName;
        }
        SourceControlManager manager = this.managers.get(scmType);
        if (manager == null) {
            LOG.error("Unsupported SCM type: {}", scmType);
            throw new BadRequestException("Unsupported SCM type: " + scmType);
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

    public String canonicalizeRepository(String scmType, String repo) {
        SourceControlManager manager = getSourceControlManager(scmType);
        return manager instanceof GithubManager ? GithubManager.canonicalizeRepository(repo) : repo;
    }

    /**
     * Returns the configured repository identity for authorization. Escaping the SCM key keeps
     * separator characters and literal percent escapes distinct from repository names.
     */
    public String getRepositoryResourceName(String scmType, String repo) {
        if (StringUtils.isEmpty(repo)) {
            throw new BadRequestException("A repository is required");
        }
        String configuredScm = StringUtils.isEmpty(scmType) ? defaultScmTypeName : scmType;
        getSourceControlManager(configuredScm);
        if (managers instanceof SortedMap) {
            // Honor the manager map's comparator, including production's case-insensitive lookup.
            configuredScm =
                    ((SortedMap<String, SourceControlManager>) managers)
                            .tailMap(configuredScm)
                            .firstKey();
        }
        String resourceName =
                SCM_KEY_ESCAPER.escape(configuredScm)
                        + "/"
                        + canonicalizeRepository(configuredScm, repo);
        if (resourceName.codePointCount(0, resourceName.length())
                > MAX_REPOSITORY_RESOURCE_NAME_LENGTH) {
            throw new BadRequestException(
                    "SCM repository resource name must not exceed 175 characters");
        }
        return resourceName;
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

    public List<CommitBean> getCommits(
            String scmType, String repo, String startSha, String endSha, int size)
            throws Exception {
        return getSourceControlManager(scmType).getCommits(repo, startSha, endSha, size);
    }

    public List<CommitBean> getCommits(
            String scmType, String repo, String startSha, String endSha, int size, String path)
            throws Exception {
        return getSourceControlManager(scmType).getCommits(repo, startSha, endSha, size, path);
    }
}
