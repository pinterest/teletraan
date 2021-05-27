/**
 * Copyright 2016 Pinterest, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.bean;

import com.pinterest.deployservice.common.DeployInternalException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;
import javax.validation.constraints.Pattern;

/**
 * Keep the bean and table in sync
 * <p>
 * CREATE TABLE environs (
 * env_id        VARCHAR(22)         NOT NULL,
 * env_name      VARCHAR(64)         NOT NULL,
 * stage_name    VARCHAR(64)         NOT NULL,
 * env_state     VARCHAR(32)         NOT NULL,
 * description   VARCHAR(1024),
 * build_name    VARCHAR(64),
 * branch        VARCHAR(64),
 * chatroom      VARCHAR(128),
 * deploy_id     VARCHAR(22),
 * deploy_type   VARCHAR(32),
 * max_parallel  INT                 NOT NULL,
 * priority      VARCHAR(16)         NOT NULL,
 * system_priority  INT,
 * stuck_th      INT                 NOT NULL,
 * success_th    INT                 NOT NULL,
 * adv_config_id VARCHAR(22),
 * sc_config_id  VARCHAR(22),
 * last_operator VARCHAR(64)         NOT NULL,
 * last_update   BIGINT              NOT NULL,
 * acc_type      VARCHAR(32)         NOT NULL,
 * email_recipients VARCHAR(1024),
 * watch_recipients VARCHAR(1024),
 * metrics_config_id VARCHAR(22),
 * alarm_config_id     VARCHAR(22),
 * webhooks_config_id  VARCHAR(22),
 * max_deploy_num      INT           NOT NULL,
 * max_deploy_day      INT           NOT NULL,
 * is_docker           TINYINT(1)    DEFAULT 0,
 * max_parallel_pct    TINYINT(1)    NOT NULL DEFAULT 0,
 * state               VARCHAR(32)         NOT NULL,
 * cluster_name        VARCHAR(128)
 * max_parallel_rp     INT           NOT NULL DEFAULT 1,
 * schedule_id         VARCHAR(22)   DEFAULT NULL,
 * deploy_constraint_id VARCHAR(22)   DEFAULT NULL,
 * external_id CHAR(36),
 * allow_private_build TINYINT(1)    DEFAULT 0,
 * ensure_trusted_build TINYINT(1)    DEFAULT 0,
 * stage_type VARCHAR(32) NOT NULL DEFAULT PRODUCTION,
 * is_sox TINYINT(1) NOT NULL DEFAULT 0,
 * <p>
 * PRIMARY KEY   (env_id)
 * );
 */
public class EnvironBean implements Updatable, Serializable {
    @JsonProperty("id")
    private String env_id;

    @NotEmpty
    @JsonProperty("envName")
    private String env_name;

    @NotEmpty
    @JsonProperty("stageName")
    private String stage_name;

    @JsonProperty("envState")
    private EnvState env_state;

    @JsonProperty("description")
    private String description;

    @JsonProperty("buildName")
    private String build_name;

    @JsonProperty("branch")
    private String branch;

    @JsonProperty("chatroom")
    private String chatroom;

    @JsonProperty("deployId")
    private String deploy_id;

    @JsonProperty("deployType")
    private DeployType deploy_type;

    @JsonProperty("maxParallel")
    private Integer max_parallel;

    @JsonProperty("priority")
    private DeployPriority priority;

    @JsonProperty("systemPriority")
    private Integer system_priority;

    @JsonProperty("stuckThreshold")
    private Integer stuck_th;

    @JsonProperty("successThreshold")
    private Integer success_th;

    @JsonProperty("advancedConfigId")
    private String adv_config_id;

    @JsonProperty("scriptConfigId")
    private String sc_config_id;

    @JsonProperty("lastOperator")
    private String last_operator;

    @JsonProperty("lastUpdate")
    private Long last_update;

    @JsonProperty("acceptanceType")
    private AcceptanceType accept_type;

    @JsonProperty("emailRecipients")
    private String email_recipients;

    @JsonProperty("notifyAuthors")
    private Boolean notify_authors;

    @JsonProperty("watchRecipients")
    private String watch_recipients;

    @JsonProperty("metricsConfigId")
    private String metrics_config_id;

    @JsonProperty("alarmConfigId")
    private String alarm_config_id;

    @JsonProperty("webhooksConfigId")
    private String webhooks_config_id;

    @JsonProperty("maxDeployNum")
    private Integer max_deploy_num;

    @JsonProperty("maxDeployDay")
    private Integer max_deploy_day;

    @JsonProperty("isDocker")
    private Boolean is_docker;

    @Range(min = 0, max = 100)
    @JsonProperty("maxParallelPct")
    private Integer max_parallel_pct;

    @JsonProperty("state")
    private EnvironState state;

    @JsonProperty("clusterName")
    private String cluster_name;

    @JsonProperty("maxParallelRp")
    private Integer max_parallel_rp;

    @JsonProperty("overridePolicy")
    private OverridePolicy override_policy;

    @JsonProperty("scheduleId")
    private String schedule_id;

    @JsonProperty("deployConstraintId")
    private String deploy_constraint_id;

    @JsonProperty("externalId")
    private String external_id;

    @JsonProperty("allowPrivateBuild")
    private Boolean allow_private_build;

    @JsonProperty("ensureTrustedBuild")
    private Boolean ensure_trusted_build;

    @JsonProperty("stageType")
    private EnvType stage_type;

    @JsonProperty("isSOX")
    private Boolean is_sox;

    public void validate() throws Exception {
        // A bunch of these fields will always be alphanumeric (with _ and -)
        String envRegEx = "^[A-Za-z0-9_\\-]*$";
        if (this.env_name != null && !this.env_name.matches(envRegEx)) {
            throw new IllegalArgumentException(String.format("Environment name must match regex %s", envRegEx));
        }
        String stageRegEx = "^[A-Za-z0-9_\\-]*$";
        if (this.stage_name != null && !this.stage_name.matches(stageRegEx)) {
            throw new IllegalArgumentException(String.format("Stage name must match regex %s", stageRegEx));
        }
        String buildRegEx = "^[A-Za-z0-9_\\.\\/\\-]*$";
        if (this.build_name != null && !this.build_name.matches(buildRegEx)) {
            throw new IllegalArgumentException(String.format("Build name must match regex %s", buildRegEx));
        }
        String branchRegEx = "^[A-Za-z0-9_\\:\\.\\,\\/\\-]*$";
        if (this.branch != null && !this.branch.matches(branchRegEx)) {
            throw new IllegalArgumentException(String.format("Branch name must match regex %s", branchRegEx));
        }
        String chatRegex = "^[A-Za-z0-9_ \\#\\,\\-]*$";
        if (this.chatroom != null && !this.chatroom.matches(chatRegex)) {
            throw new IllegalArgumentException(String.format("Chatroom must match regex %s", chatRegex));
        }
    }

    public String getWebhooks_config_id() {
        return webhooks_config_id;
    }

    public void setWebhooks_config_id(String webhook_config_id) {
        this.webhooks_config_id = webhook_config_id;
    }

    public String getEnv_id() {
        return env_id;
    }

    public void setEnv_id(String env_id) {
        this.env_id = env_id;
    }

    public String getEnv_name() {
        return env_name;
    }

    public void setEnv_name(String env_name) {
        this.env_name = env_name;
    }

    public String getStage_name() {
        return stage_name;
    }

    public void setStage_name(String stage_name) {
        this.stage_name = stage_name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        // Escape user input which could contain injected Javascript
        this.description = StringEscapeUtils.escapeHtml(description);
    }

    public String getBuild_name() {
        return build_name;
    }

    public void setBuild_name(String build_name) {
        this.build_name = build_name;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getChatroom() {
        return chatroom;
    }

    public void setChatroom(String chatroom) {
        this.chatroom = chatroom;
    }

    public String getDeploy_id() {
        return deploy_id;
    }

    public void setDeploy_id(String deploy_id) {
        this.deploy_id = deploy_id;
    }

    public Integer getMax_parallel() {
        return max_parallel;
    }

    public void setMax_parallel(Integer max_parallel) {
        this.max_parallel = max_parallel;
    }

    public Integer getStuck_th() {
        return stuck_th;
    }

    public void setStuck_th(Integer stuck_th) {
        this.stuck_th = stuck_th;
    }

    public Integer getSuccess_th() {
        return success_th;
    }

    public void setSuccess_th(Integer success_th) {
        this.success_th = success_th;
    }

    public String getAdv_config_id() {
        return adv_config_id;
    }

    public void setAdv_config_id(String adv_config_id) {
        this.adv_config_id = adv_config_id;
    }

    public String getSc_config_id() {
        return sc_config_id;
    }

    public void setSc_config_id(String sc_config_id) {
        this.sc_config_id = sc_config_id;
    }

    public String getLast_operator() {
        return last_operator;
    }

    public void setLast_operator(String last_operator) {
        this.last_operator = last_operator;
    }

    public Long getLast_update() {
        return last_update;
    }

    public void setLast_update(Long last_update) {
        this.last_update = last_update;
    }

    public EnvState getEnv_state() {
        return env_state;
    }

    public void setEnv_state(EnvState env_state) {
        this.env_state = env_state;
    }

    public DeployType getDeploy_type() {
        return deploy_type;
    }

    public void setDeploy_type(DeployType deploy_type) {
        this.deploy_type = deploy_type;
    }

    public DeployPriority getPriority() {
        return priority;
    }

    public void setPriority(DeployPriority priority) {
        this.priority = priority;
    }

    public Integer getSystem_priority() {
        return system_priority;
    }

    public void setSystem_priority(Integer system_priority) {
        this.system_priority = system_priority;
    }

    public AcceptanceType getAccept_type() {
        return accept_type;
    }

    public void setAccept_type(AcceptanceType accept_type) {
        this.accept_type = accept_type;
    }

    public String getEmail_recipients() {
        return email_recipients;
    }

    public void setEmail_recipients(String email_recipients) {
        this.email_recipients = email_recipients;
    }

    public boolean getNotify_authors() {
        return notify_authors;
    }

    public void setNotify_authors(boolean notify_authors) {
        this.notify_authors = notify_authors;
    }

    public String getWatch_recipients() {
        return watch_recipients;
    }

    public void setWatch_recipients(String watch_recipients) {
        this.watch_recipients = watch_recipients;
    }

    public String getMetrics_config_id() {
        return metrics_config_id;
    }

    public void setMetrics_config_id(String metrics_config_id) {
        this.metrics_config_id = metrics_config_id;
    }

    public String getAlarm_config_id() {
        return alarm_config_id;
    }

    public void setAlarm_config_id(String alarm_config_id) {
        this.alarm_config_id = alarm_config_id;
    }

    public Integer getMax_deploy_num() {
        return max_deploy_num;
    }

    public void setMax_deploy_num(Integer max_deploy_num) {
        this.max_deploy_num = max_deploy_num;
    }

    public Integer getMax_deploy_day() {
        return max_deploy_day;
    }

    public void setMax_deploy_day(Integer max_deploy_day) {
        this.max_deploy_day = max_deploy_day;
    }

    public Boolean getIs_docker() {
        return is_docker;
    }

    public void setIs_docker(Boolean is_docker) {
        this.is_docker = is_docker;
    }

    public Integer getMax_parallel_pct() {
        return max_parallel_pct;
    }

    public void setMax_parallel_pct(Integer max_parallel_pct) {
        this.max_parallel_pct = max_parallel_pct;
    }

    public EnvironState getState() {
        return state;
    }

    public void setState(EnvironState state) {
        this.state = state;
    }

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public Integer getMax_parallel_rp() {
        return max_parallel_rp;
    }

    public void setMax_parallel_rp(Integer max_parallel_rp) {
        this.max_parallel_rp = max_parallel_rp;
    }

    public OverridePolicy getOverride_policy() {
        return override_policy;
    }

    public void setOverride_policy(OverridePolicy override_policy) {
        this.override_policy = override_policy;
    }

    public String getSchedule_id() {
        return schedule_id;
    }

    public void setSchedule_id(String schedule_id) {
        this.schedule_id = schedule_id;
    }

    public String getDeploy_constraint_id() {
        return deploy_constraint_id;
    }

    public void setDeploy_constraint_id(String deploy_constraint_id) {
        this.deploy_constraint_id = deploy_constraint_id;
    }

    public String getExternal_id() {
        return external_id;
    }

    public void setExternal_id(String external_id) {
        this.external_id = external_id;
    }

    public Boolean getAllow_private_build() {
        return allow_private_build;
    }

    public void setAllow_private_build(Boolean allow_private_build) {
        this.allow_private_build = allow_private_build;
    }

    public Boolean getEnsure_trusted_build() {
        return ensure_trusted_build;
    }

    public void setEnsure_trusted_build(Boolean ensure_trusted_build) {
        this.ensure_trusted_build = ensure_trusted_build;
    }

    public EnvType getStage_type() {
        return stage_type;
    }

    public void setStage_type(EnvType stage_type) {
        this.stage_type = stage_type;
    }

    public Boolean getIs_sox() {
        return is_sox;
    }

    public void setIs_sox(Boolean is_sox) {
        this.is_sox = is_sox;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("env_id", env_id);
        clause.addColumn("env_name", env_name);
        clause.addColumn("stage_name", stage_name);
        clause.addColumn("env_state", env_state);
        clause.addColumn("description", description);
        clause.addColumn("build_name", build_name);
        clause.addColumn("branch", branch);
        clause.addColumn("chatroom", chatroom);
        clause.addColumn("deploy_id", deploy_id);
        clause.addColumn("deploy_type", deploy_type);
        clause.addColumn("max_parallel", max_parallel);
        clause.addColumn("priority", priority);
        clause.addColumn("system_priority", system_priority);
        clause.addColumn("stuck_th", stuck_th);
        clause.addColumn("success_th", success_th);
        clause.addColumn("adv_config_id", adv_config_id);
        clause.addColumn("sc_config_id", sc_config_id);
        clause.addColumn("last_operator", last_operator);
        clause.addColumn("last_update", last_update);
        clause.addColumn("accept_type", accept_type);
        clause.addColumn("email_recipients", email_recipients);
        clause.addColumn("notify_authors", notify_authors);
        clause.addColumn("watch_recipients", watch_recipients);
        clause.addColumn("metrics_config_id", metrics_config_id);
        clause.addColumn("alarm_config_id", alarm_config_id);
        clause.addColumn("webhooks_config_id", webhooks_config_id);
        clause.addColumn("max_deploy_num", max_deploy_num);
        clause.addColumn("max_deploy_day", max_deploy_day);
        clause.addColumn("is_docker", is_docker);
        clause.addColumn("max_parallel_pct", max_parallel_pct);
        clause.addColumn("state", state);
        clause.addColumn("cluster_name", cluster_name);
        clause.addColumn("max_parallel_rp", max_parallel_rp);
        clause.addColumn("override_policy", override_policy);
        clause.addColumn("schedule_id", schedule_id);
        clause.addColumn("deploy_constraint_id", deploy_constraint_id);
        clause.addColumn("external_id", external_id);
        clause.addColumn("allow_private_build", allow_private_build);
        clause.addColumn("ensure_trusted_build", ensure_trusted_build);
        clause.addColumn("stage_type", stage_type);
        clause.addColumn("is_sox", is_sox);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
