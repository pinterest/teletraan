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
import com.pinterest.deployservice.common.DeployInternalException;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhabricatorManager extends BaseManager {

    private static final Logger LOG = LoggerFactory.getLogger(PhabricatorManager.class);
    public static final String TYPE = "Phabricator";
    private static final
    String
        QUERY_COMMITS_HISTORY_PARAMETER =
        "{\"commit\":\"%s\", \"limit\":%d, \"repository\":\"%s\"}";
    private static final
    String
        QUERY_COMMITS_HISTORY_PARAMETER_WITH_PATH =
        "{\"commit\":\"%s\", \"limit\":%d, \"repository\":\"%s\", \"path\":\"%s\"}";
    private static final int DEFAULT_SIZE = 30;
    private static final String DEFAULT_PATH = "*";
    private static final String ARC_OUTPUT_NOTICE = "Waiting for JSON parameters on stdin...";
    private static final int ARC_OUTPUT_NOTICE_LEN = ARC_OUTPUT_NOTICE.length();
    private static final String UNKNOWN = "UNKNOWN";
    private static final Pattern AUTHOR_NAME_PATTERN = Pattern.compile("(.*)<(.*)@(.*)>");

    private String urlPrefix;
    private String arcLocation;
    private String arcrcLocation;

    public PhabricatorManager(String urlPrefix, String arcLocation, String arcrcLocation) {
        this.urlPrefix = urlPrefix;
        this.arcLocation = arcLocation;
        this.arcrcLocation = arcrcLocation;
    }

    private Map<String, Object> queryCLI(String input) throws Exception {
        ProcessBuilder builder;
        if (StringUtils.isEmpty(arcrcLocation)) {
            builder = new ProcessBuilder(arcLocation, "call-conduit",
                String.format("--conduit-uri=%s", urlPrefix), "diffusion.historyquery");
        } else {
            builder = new ProcessBuilder(arcLocation, "call-conduit",
                String.format("--conduit-uri=%s", urlPrefix), "diffusion.historyquery",
                "--arcrc-file", arcrcLocation);
        }
        LOG.debug("Execute arc command: \n{}", builder.command());

        // Redirects error stream to output stream, so have only one input stream to read from
        builder.redirectErrorStream(true);

        // Run command
        Process process = builder.start();

        // Feed the input parameters
        BufferedWriter
            writer =
            new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        writer.write(input);
        writer.flush();
        writer.close();

        InputStream stdout = process.getInputStream();
        String line;
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

        // Read response
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        reader.close();

        // We will remove "Waiting for JSON parameters on stdin..." from the output if exists
        String output = sb.toString();
        if (output.startsWith(ARC_OUTPUT_NOTICE)) {
            output = output.substring(ARC_OUTPUT_NOTICE_LEN);
        }
        LOG.debug("arc command output is: \n{}", output);

        GsonBuilder gson = new GsonBuilder();
        return gson.create().fromJson(output, new TypeToken<HashMap<String, Object>>() {
        }.getType());
    }

    private String getAuthorHandle(String authorName) {
        /* Phabricator authorName is in the following format, we extract and return the email
           handle: "foo bar <fb@xyz.com>"
        */
        Matcher m = AUTHOR_NAME_PATTERN.matcher(authorName);
        if (m.matches()) {
            return m.group(2);
        }
        return "UNKNOWN";
    }

    private CommitBean toCommitBean(Map<String, Object> commitMap, String callsign)
        throws Exception {
        CommitBean CommitBean = new CommitBean();

        @SuppressWarnings("unchecked")
        Map<String, Object> metaData = (Map<String, Object>) commitMap.get("commit");
        String sha = metaData.get("commitIdentifier").toString();
        CommitBean.setSha(sha);
        CommitBean.setInfo(generateCommitLink(callsign, sha));

        Object p = metaData.get("epoch");
        if (p == null) {
            CommitBean.setDate(0L);
        } else {
            CommitBean.setDate(Long.parseLong(p.toString()) * 1000);
        }

        Object o = commitMap.get("commitData");
        if (o instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> commitData = (Map<String, Object>) commitMap.get("commitData");
            String authorName = getAuthorHandle(commitData.get("authorName").toString());
            CommitBean.setAuthor(authorName);
            String message = commitData.get("commitMessage").toString();
            String[] parts = message.split("\n", 2);
            CommitBean.setTitle(parts[0]);
            if (parts.length > 1) {
                CommitBean.setMessage(parts[1]);
            }
        } else {
            CommitBean.setAuthor(UNKNOWN);
            CommitBean.setTitle(UNKNOWN);
        }

        return CommitBean;
    }

    @Override
    public String generateCommitLink(String repo, String sha) {
        return String.format("%s/r%s%s", urlPrefix, repo, sha);
    }

    @Override
    public String getCommitLinkTemplate() {
        return String.format("%s/r%%s%%s", urlPrefix);
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
        String input = String.format(QUERY_COMMITS_HISTORY_PARAMETER, sha, 1, repo);
        Map<String, Object> json = queryCLI(input);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) json.get("response");
            @SuppressWarnings("unchecked")
            ArrayList<Map<String, Object>>
                commitsArray =
                (ArrayList<Map<String, Object>>) response.get("pathChanges");

            return toCommitBean(commitsArray.get(0), repo);
        } catch (Exception e) {
            if (json.get("response") == null) {
                throw new Exception(json.get("errorMessage").toString());
            } else {
                throw new Exception(e.getMessage());
            }
        }
    }

    @Override
    public Queue<CommitBean> getCommits(String repo, String startSha, boolean keepHead, String path)
        throws Exception {
        String input = String.format(QUERY_COMMITS_HISTORY_PARAMETER_WITH_PATH, startSha, DEFAULT_SIZE, repo, path);
        Map<String, Object> json = queryCLI(input);
        @SuppressWarnings("unchecked")
        Map<String, Object> commitsJson = (Map<String, Object>) json.get("response");
        @SuppressWarnings("unchecked")
        ArrayList<Map<String, Object>>
            commitsArray =
            (ArrayList<Map<String, Object>>) commitsJson.get("pathChanges");

        if (!commitsArray.isEmpty()) {
            Queue<CommitBean> CommitBeans = new LinkedList<>();
            for (Map<String, Object> commitMap : commitsArray) {
                if (!keepHead) {
                    keepHead = true;
                    continue;
                }
                CommitBeans.offer(toCommitBean(commitMap, repo));
            }
            return CommitBeans;
        } else {
            throw new DeployInternalException(
                "Invalid SHA or branch name passed to Phabricator getCommitBeans!");
        }

    }
}
