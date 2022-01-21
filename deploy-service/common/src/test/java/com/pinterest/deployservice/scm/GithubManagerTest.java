package com.pinterest.deployservice.scm;

import java.util.List;

import com.pinterest.deployservice.bean.CommitBean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/* To run this test only: 
 *  update the github configuration in setUp() before running. uncomment the ignore
 *  $ mvn -Dtest=GithubManagerTest -DfailIfNoTests=false test
 */
public class GithubManagerTest {
    GithubManager manager;

    @Before
    public void setUp() throws Exception {
        String apiPrefix = "https://api.github.com";
        String urlPrefix = "https://github.com";
        String appId = "yourAppId";
        String appPrivateKeyKnox = "";
        String appOrgnization = "yourOrg";
        String token = "";   // github personal token
        
        this.manager = new GithubManager(token, appId, appPrivateKeyKnox, appOrgnization, apiPrefix, urlPrefix);
        Assert.assertEquals(this.manager.getUrlPrefix(), urlPrefix);
    }

    @Test
    @Ignore
    public void testGetCommit() throws Exception {
        CommitBean commit = this.manager.getCommit("pinterest/teletraan", "0caa0c0dc877920811e0eb695f1ed0dfd498f586");
        System.out.println(commit);
        Assert.assertEquals("https://github.com/pinterest/teletraan/commit/0caa0c0dc877920811e0eb695f1ed0dfd498f586", commit.getInfo());

        List <CommitBean> commits = this.manager.getCommits("pinterest/teletraan", "a220f1808b38f27314b4f5b89270216de7fb84bc", "7fa731d99b9c98c29ce500dfef64ea32c3641786", 100);
        System.out.println(commits);
        System.out.println("size = " + commits.size());
    }
}
