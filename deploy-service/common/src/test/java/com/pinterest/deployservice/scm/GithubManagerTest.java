/**
 * Copyright (c) 2022-2025 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pinterest.deployservice.bean.CommitBean;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/* To run this test only:
 *  update the github configuration in setUp() before running. uncomment the ignore
 *  $ mvn -Dtest=GithubManagerTest -DfailIfNoTests=false test
 */
class GithubManagerTest {
    GithubManager manager;

    @BeforeEach
    void setUp() {
        String typeName = "Github";
        String apiPrefix = "https://api.github.com";
        String urlPrefix = "https://github.com";
        String appId = "yourAppId";
        String appPrivateKeyKnox = "";
        String appOrgnization = "yourOrg";
        String token = ""; // github personal token

        this.manager =
                new GithubManager(
                        token,
                        appId,
                        appPrivateKeyKnox,
                        appOrgnization,
                        typeName,
                        apiPrefix,
                        urlPrefix);
        assertEquals(this.manager.getUrlPrefix(), urlPrefix);
    }

    @Test
    @Disabled("Github manager is not ready for test")
    void testGetCommit() throws Exception {
        CommitBean commit =
                this.manager.getCommit(
                        "pinterest/teletraan", "0caa0c0dc877920811e0eb695f1ed0dfd498f586");
        System.out.println(commit);
        assertEquals(
                "https://github.com/pinterest/teletraan/commit/0caa0c0dc877920811e0eb695f1ed0dfd498f586",
                commit.getInfo());

        List<CommitBean> commits =
                this.manager.getCommits(
                        "pinterest/teletraan",
                        "a220f1808b38f27314b4f5b89270216de7fb84bc",
                        "7fa731d99b9c98c29ce500dfef64ea32c3641786",
                        100);
        System.out.println(commits);
        System.out.println("size = " + commits.size());
    }
}
