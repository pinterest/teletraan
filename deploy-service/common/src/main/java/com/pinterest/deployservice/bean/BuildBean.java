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
package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 *
 * <p>CREATE TABLE builds ( build_id VARCHAR(30) NOT NULL, build_name VARCHAR(64) NOT NULL,
 * artifact_url VARCHAR(512) NOT NULL, scm VARCHAR(64), scm_repo VARCHAR(64) NOT NULL, scm_branch
 * VARCHAR(64) NOT NULL, scm_commit_7 VARCHAR(7) NOT NULL, scm_commit VARCHAR(64) NOT NULL,
 * commit_date BIGINT NOT NULL, publish_info VARCHAR(512) NOT NULL, publish_date BIGINT NOT NULL,
 * publisher VARCHAR(64), scm_info VARCHAR(512), PRIMARY KEY (build_id) );
 */
public class BuildBean implements Updatable {
    @JsonProperty("id")
    private String build_id;

    @NotEmpty
    @JsonProperty("name")
    private String build_name;

    @NotEmpty
    @JsonProperty("artifactUrl")
    private String artifact_url;

    @JsonProperty("type")
    private String scm;

    @NotEmpty
    @JsonProperty("repo")
    private String scm_repo;

    @NotEmpty
    @JsonProperty("branch")
    private String scm_branch;

    @NotEmpty
    @JsonProperty("commit")
    private String scm_commit;

    @JsonProperty("commitShort")
    private String scm_commit_7;

    @JsonProperty("commitInfo")
    private String scm_info;

    @JsonProperty("commitDate")
    private Long commit_date;

    @JsonProperty("publishInfo")
    private String publish_info;

    @JsonProperty("publisher")
    private String publisher;

    @JsonProperty("publishDate")
    private Long publish_date;

    public String getScm_info() {
        return scm_info;
    }

    public void setScm_info(String scm_info) {
        this.scm_info = scm_info;
    }

    public String getBuild_id() {
        return build_id;
    }

    public void setBuild_id(String build_id) {
        this.build_id = build_id;
    }

    public String getBuild_name() {
        return build_name;
    }

    public void setBuild_name(String build_name) {
        this.build_name = build_name;
    }

    public String getArtifact_url() {
        return artifact_url;
    }

    public void setArtifact_url(String artifact_url) {
        this.artifact_url = artifact_url;
    }

    public String getScm() {
        return scm;
    }

    public void setScm(String scm) {
        this.scm = scm;
    }

    public String getScm_repo() {
        return scm_repo;
    }

    public void setScm_repo(String scm_repo) {
        this.scm_repo = scm_repo;
    }

    public String getScm_branch() {
        return scm_branch;
    }

    public void setScm_branch(String scm_branch) {
        this.scm_branch = scm_branch;
    }

    public String getScm_commit_7() {
        return scm_commit_7;
    }

    public void setScm_commit_7(String scm_commit_7) {
        this.scm_commit_7 = scm_commit_7;
    }

    public String getScm_commit() {
        return scm_commit;
    }

    public void setScm_commit(String scm_commit) {
        this.scm_commit = scm_commit;
    }

    public Long getCommit_date() {
        return commit_date;
    }

    public void setCommit_date(Long commit_date) {
        this.commit_date = commit_date;
    }

    public String getPublish_info() {
        return publish_info;
    }

    public void setPublish_info(String publish_info) {
        this.publish_info = publish_info;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Long getPublish_date() {
        return publish_date;
    }

    public void setPublish_date(Long publish_date) {
        this.publish_date = publish_date;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("build_id", build_id);
        clause.addColumn("build_name", build_name);
        clause.addColumn("artifact_url", artifact_url);
        clause.addColumn("scm", scm);
        clause.addColumn("scm_repo", scm_repo);
        clause.addColumn("scm_branch", scm_branch);
        clause.addColumn("scm_commit_7", scm_commit_7);
        clause.addColumn("scm_commit", scm_commit);
        clause.addColumn("commit_date", commit_date);
        clause.addColumn("publish_info", publish_info);
        clause.addColumn("publisher", publisher);
        clause.addColumn("publish_date", publish_date);
        clause.addColumn("scm_info", scm_info);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
