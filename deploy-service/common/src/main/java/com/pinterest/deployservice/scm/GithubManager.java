/**
 * Copyright 2016 Pinterest, Inc.
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
import com.pinterest.deployservice.common.HTTPClient;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class GithubManager extends BaseManager {

    public final static String TYPE = "Github";
    private final static String UNKNOWN_LOGIN = "UNKNOWN";
    private String apiPrefix;
    private String urlPrefix;

    private Map<String, String> headers = new HashMap<String, String>();

    public GithubManager(String token, String apiPrefix, String urlPrefix) {
        this.apiPrefix = apiPrefix;
        this.urlPrefix = urlPrefix;
        headers.put("Authorization", String.format("token %s", token));
    }

    private String getSha(Map<String, Object> jsonMap) {
        return (String) jsonMap.get("sha");
    }

    private String getLogin(Map<String, Object> jsonMap) {
        Map<String, Object> authorMap = (Map<String, Object>) jsonMap.get("author");
        if (authorMap != null) {
            return (String) authorMap.get("login");
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
    public String getType() {
        return TYPE;
    }

    @Override
    public CommitBean getCommit(String repo, String sha) throws Exception {
        try
        {
            HTTPClient httpClient = new HTTPClient();
            String url = String.format("%s/repos/%s/commits/%s", apiPrefix, repo, sha);

            // TODO: Do not RETRY since it will timeout the thrift caller, need to revisit
            String jsonPayload = httpClient.get(url, null, null, headers, 1);
            GsonBuilder builder = new GsonBuilder();
            Map<String, Object>
                jsonMap =
                builder.create().fromJson(jsonPayload, new TypeToken<HashMap<String, Object>>() {
                }.getType());
            return toCommitBean(jsonMap, repo);
        }
        catch (Exception e)
        {
            throw new Exception("The startsha or endsha is not a correct public commit")
        }
    }

    @Override
    public Queue<CommitBean> getCommits(String repo, String startSha, boolean keepHead, String path)
        throws Exception {
        HTTPClient httpClient = new HTTPClient();
        String url = String.format("%s/repos/%s/commits", apiPrefix, repo);

        // TODO: Do not RETRY since it will timeout the thrift caller, need to revisit
        Map<String, String> params = new HashMap<String, String>();
        params.put("sha", startSha);

        String jsonPayload = httpClient.get(url, null, params, headers, 1);
        Queue<CommitBean> CommitBeans = new LinkedList<CommitBean>();
        GsonBuilder builder = new GsonBuilder();
        Map<String, Object>[]
            jsonMaps =
            builder.create().fromJson(jsonPayload, new TypeToken<HashMap<String, Object>[]>() {
            }.getType());

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
