/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.CommitBean;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class DefaultSourceControlManager implements SourceControlManager {
    private static final String DEFAULT_PATH = "";

    @Override
    public String generateCommitLink(String repo, String sha) {
        return "";
    }

    @Override
    public String getCommitLinkTemplate() {
        return "";
    }

    @Override
    public String getUrlPrefix() {
        return "";
    }

    @Override
    public String getTypeName() {
        return "UNKOWN";
    }

    @Override
    public CommitBean getCommit(String repo, String sha) throws Exception {
        return null;
    }

    public Queue<CommitBean> getCommits(String repo, String sha, boolean keepHead)
            throws Exception {
        return getCommits(repo, sha, keepHead, DEFAULT_PATH);
    }

    @Override
    public Queue<CommitBean> getCommits(String repo, String sha, boolean keepHead, String path)
            throws Exception {
        return new LinkedList<>();
    }

    public List<CommitBean> getCommits(String repo, String startSha, String endSha, int size)
            throws Exception {
        return getCommits(repo, startSha, endSha, size, DEFAULT_PATH);
    }

    @Override
    public List<CommitBean> getCommits(
            String repo, String startSha, String endSha, int size, String path) throws Exception {
        return Collections.emptyList();
    }
}
