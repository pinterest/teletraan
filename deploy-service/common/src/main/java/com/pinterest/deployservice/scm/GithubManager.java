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

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pinterest.deployservice.bean.CommitBean;
import com.pinterest.deployservice.common.EncryptionUtils;
import com.pinterest.deployservice.common.KeyReader;
import com.pinterest.deployservice.common.KnoxKeyReader;
import com.pinterest.teletraan.universal.http.HttpClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GithubManager extends BaseManager {
    private static final Logger LOG = LoggerFactory.getLogger(GithubManager.class);
    private static final String UNKNOWN_LOGIN = "UNKNOWN";
    private static final long TOKEN_TTL_MILLIS = 600000; // token expires after 10 minutes
    private final String apiPrefix;
    private final String urlPrefix;
    private final String githubAppId;
    private String githubAppPrivateKeyKnox = "github:pinternal:app:teletraan";
    private final String githubAppOrganization;
    private final String token;
    private final HttpClient httpClient;
    private final KeyReader knoxKeyReader = new KnoxKeyReader();

    public GithubManager(
            String token,
            String appId,
            String appPrivateKeyKnox,
            String appOrganization,
            String typeName,
            String apiPrefix,
            String urlPrefix) {
        this.typeName = typeName;
        this.apiPrefix = apiPrefix;
        this.urlPrefix = urlPrefix;
        this.githubAppId = appId;
        this.githubAppPrivateKeyKnox = appPrivateKeyKnox;
        this.githubAppOrganization = appOrganization;
        this.token = token;
        LOG.debug("[Teletraan-fork][Buildkite] Trying to fetch key " + githubAppPrivateKeyKnox);
        if (StringUtils.isNotBlank(githubAppPrivateKeyKnox)) {
            // get private key PEM from knox
            knoxKeyReader.init(githubAppPrivateKeyKnox);
        }
        httpClient =
                HttpClient.builder()
                        .authorizationSupplier(this::fetchAuthorizationHeader)
                        .maxRetries(1)
                        .build();
    }

    private String fetchAuthorizationHeader() {
        // if token is specified, use token auth, otherwise, use GitHub app auth
        if (StringUtils.isBlank(this.token)) {
            try {
                String githubAppPrivateKey = knoxKeyReader.getKey();
                if (StringUtils.isBlank(githubAppPrivateKey)) {
                    throw new IllegalStateException("Github Knox key is blank");
                }

                // generate jwt token by signing with GitHub app id and private key
                String jwtToken =
                        EncryptionUtils.createGithubJWT(
                                this.githubAppId, githubAppPrivateKey, TOKEN_TTL_MILLIS);

                // get installation token using the jwt token
                GitHub gitHubApp = new GitHubBuilder().withJwtToken(jwtToken).build();
                GHAppInstallation appInstallation =
                        gitHubApp
                                .getApp()
                                .getInstallationByOrganization(this.githubAppOrganization);
                GHAppInstallationToken appInstallationToken =
                        appInstallation.createToken().create();

                // always use the newly created GitHub app token as the token will expire
                return String.format("Token %s", appInstallationToken.getToken());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to get Github token", e);
            }
        } else {
            // this is the token that does not expire
            return String.format("Token %s", this.token);
        }
    }

    private String getSha(Map<String, Object> jsonMap) {
        return (String) jsonMap.get("sha");
    }

    private String getLogin(Map<String, Object> jsonMap) {
        Map<String, Object> authorMap = (Map<String, Object>) jsonMap.get("author");
        if (authorMap != null) {
            return (String) authorMap.get("login");
        }
        // for some commits, the author info is under "commit"
        Map<String, Object> commitMap = (Map<String, Object>) jsonMap.get("commit");
        if (commitMap != null) {
            authorMap = (Map<String, Object>) commitMap.get("author");
            if (authorMap != null) {
                String email = (String) authorMap.get("email");
                return email.split("@")[0];
            }
        }

        return UNKNOWN_LOGIN;
    }

    private long getDate(Map<String, Object> jsonMap) {
        Map<String, Object> commiterMap = (Map<String, Object>) jsonMap.get("committer");
        String dateGMTStr = (String) commiterMap.get("date");
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis();
        DateTime dt = parser.parseDateTime(dateGMTStr);
        return dt.getMillis();
    }

    private String getMessage(Map<String, Object> jsonMap) {
        return (String) jsonMap.get("message");
    }

    private CommitBean toCommitBean(Map<String, Object> jsonMap, String repo) {
        CommitBean CommitBean = new CommitBean();
        String sha = getSha(jsonMap);
        CommitBean.setSha(sha);
        CommitBean.setAuthor(getLogin(jsonMap));
        Map<String, Object> commitMap = (Map<String, Object>) jsonMap.get("commit");
        CommitBean.setDate(getDate(commitMap));
        String message = getMessage(commitMap);
        String[] parts = message.split("\n", 2);
        CommitBean.setTitle(parts[0]);
        if (parts.length > 1) {
            CommitBean.setMessage(parts[1]);
        }
        CommitBean.setInfo(generateCommitLink(repo, sha));
        return CommitBean;
    }

    @Override
    public String generateCommitLink(String repo, String sha) {
        return String.format("%s/%s/commit/%s", urlPrefix, repo, sha);
    }

    @Override
    public String getCommitLinkTemplate() {
        return String.format("%s/%%s/commit/%%s", urlPrefix);
    }

    @Override
    public String getUrlPrefix() {
        return urlPrefix;
    }

    @Override
    public CommitBean getCommit(String repo, String sha) throws Exception {
        String url = String.format("%s/repos/%s/commits/%s", apiPrefix, repo, sha);

        // TODO: Do not RETRY since it will timeout the thrift caller, need to revisit
        String jsonPayload;
        try {
            jsonPayload = httpClient.get(url, null, null);
        } catch (IOException e) {
            // an IOException (and its subclasses) in this case indicates that the commit hash is
            // not found in the repo.
            // e.g. java.io.IOException: Server returned HTTP response code: 422
            LOG.warn("GitHub commit hash {} is not found in repo {}.", sha, repo);
            throw new WebApplicationException(
                    String.format("Commit hash %s is not found in repo %s", sha, repo),
                    Response.Status.NOT_FOUND);
        }

        GsonBuilder builder = new GsonBuilder();
        Map<String, Object> jsonMap =
                builder.create()
                        .fromJson(
                                jsonPayload, new TypeToken<HashMap<String, Object>>() {}.getType());
        return toCommitBean(jsonMap, repo);
    }

    @Override
    public Queue<CommitBean> getCommits(String repo, String startSha, boolean keepHead, String path)
            throws Exception {
        String url = String.format("%s/repos/%s/commits", apiPrefix, repo);

        // TODO: Do not RETRY since it will timeout the thrift caller, need to revisit
        Map<String, String> params = new HashMap<>();
        params.put("sha", startSha);
        params.put("path", path);

        String jsonPayload = httpClient.get(url, params, null);
        Queue<CommitBean> CommitBeans = new LinkedList<>();
        GsonBuilder builder = new GsonBuilder();
        Map<String, Object>[] jsonMaps =
                builder.create()
                        .fromJson(
                                jsonPayload,
                                new TypeToken<HashMap<String, Object>[]>() {}.getType());

        for (Map<String, Object> jsonMap : jsonMaps) {
            if (!keepHead) {
                keepHead = true;
                continue;
            }
            CommitBeans.offer(toCommitBean(jsonMap, repo));
        }
        return CommitBeans;
    }
}
