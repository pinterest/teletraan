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
import com.pinterest.deployservice.common.Constants;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseManager implements SourceControlManager {
    private static final Logger LOG = LoggerFactory.getLogger(BaseManager.class);
    // Max allowed commits to return
    private static final String DEFAULT_PATH = "";
    private static final int MAX_COMMITS = 500;

    protected String typeName;

    public String getTypeName() {
        return typeName;
    }

    public Queue<CommitBean> getCommits(String repo, String sha, boolean keepHead)
            throws Exception {
        return getCommits(repo, sha, keepHead, DEFAULT_PATH);
    }

    public List<CommitBean> getCommits(String repo, String startSha, String endSha, int size)
            throws Exception {
        return getCommits(repo, startSha, endSha, size, DEFAULT_PATH);
    }

    @Override
    public List<CommitBean> getCommits(
            String repo, String startSha, String endSha, int size, String path) throws Exception {
        if (size == 0) {
            size = MAX_COMMITS;
        }

        if (startSha == null) {
            startSha = Constants.DEFAULT_BRANCH_NAME;
        }

        // get a special reference commit firts
        List<CommitBean> fullCommits = new ArrayList<>();
        Queue<CommitBean> commits = new LinkedList<>();
        Queue<CommitBean> referenceCommits = new LinkedList<>();

        // get one special reference commit first, this is an optimization since most of time
        // startSha and endSha are on the same branch
        if (endSha != null) {
            CommitBean endCommit = getCommit(repo, endSha);
            referenceCommits.offer(endCommit);
        }

        boolean keepHead = true;
        while (fullCommits.size() < size) {
            // Repopulate referenceCommits, start from endSha
            if (endSha != null && referenceCommits.isEmpty()) {
                referenceCommits = getCommits(repo, endSha, keepHead, path);
            }
            // Repopulate commits, start from startSha
            if (commits.isEmpty()) {
                commits = getCommits(repo, startSha, keepHead, path);
            }

            if (keepHead) {
                keepHead = false;
            }

            if (referenceCommits.isEmpty() && commits.isEmpty()) {
                return fullCommits;
            }

            if (referenceCommits.isEmpty()) {
                CommitBean commit = commits.poll();
                fullCommits.add(commit);
                startSha = commit.getSha();
                continue;
            }

            // Since we have both commits and referenceCommits, compare the top
            CommitBean commit = commits.peek();
            CommitBean referenceCommit = referenceCommits.peek();

            // We find the endSha, or reach the endSha Date, let us return
            if (commit.getSha().equals(referenceCommit.getSha())
                    || commit.getDate() <= referenceCommit.getDate()) {
                return fullCommits;
            }

            // Handle one of the newer commit
            if (referenceCommit.getDate() == 0 || commit.getDate() <= referenceCommit.getDate()) {
                // Just bypass the newer reference commit
                endSha = referenceCommits.poll().getSha();
            } else {
                // Just take the commit
                fullCommits.add(commits.poll());
                startSha = commit.getSha();
            }
        }

        if (fullCommits.size() >= MAX_COMMITS) {
            LOG.warn(
                    "Exceeded max commits for repo={}, startSha={}, endSha={}, path={}",
                    repo,
                    startSha,
                    endSha,
                    path);
        }

        return fullCommits;
    }
}
