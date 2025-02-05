/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
import java.util.List;
import java.util.Queue;

public interface SourceControlManager {

    String generateCommitLink(String repo, String sha);

    String getCommitLinkTemplate();

    String getUrlPrefix();

    String getTypeName();

    CommitBean getCommit(String repo, String sha) throws Exception;

    // Start from sha, get default number of commits
    Queue<CommitBean> getCommits(String repo, String sha, boolean keepHead) throws Exception;

    Queue<CommitBean> getCommits(String repo, String sha, boolean keepHead, String path)
            throws Exception;

    /**
     * Returns a list of CommitInfo from startSha inclusive to endSha exclusive, or up to the
     * specified size, whichever happens first
     *
     * <p>if size == 0, then will return the full list until endSha
     *
     * <p>if endSha == null, then will return up to size, max_size = 500
     */
    List<CommitBean> getCommits(String repo, String startSha, String endSha, int size)
            throws Exception;

    List<CommitBean> getCommits(String repo, String startSha, String endSha, int size, String path)
            throws Exception;
}
