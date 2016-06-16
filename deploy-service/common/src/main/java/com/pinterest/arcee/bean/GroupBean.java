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
package com.pinterest.arcee.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * Keep the bean and table in sync
 * <p/>
 * CREATE TABLE groups (
 * group_name           VARCHAR(64)     NOT NULL,
 * last_update          BIGINT(20),     NOT NULL,
 * chatroom             VARCHAR(64),
 * email_recipients     VARCHAR(1024),
 * watch_recipients     VARCHAR(1024),
 * launch_latency_th    INT             NOT NULL DEFAULT 600,
 * healthcheck_state    TINYINT(1)      NOT NULL DEFAULT 0,
 * healthcheck_period   BIGINT          NOT NULL DEFAULT 3600,
 * lifecycle_state     TINYINT(1)      NOT NULL DEFAULT 0,
 * lifecycle_timeout   BIGINT          NOT NULL DEFAULT 600,
 * PRIMARY KEY (group_name)
 * );
 */
public class GroupBean implements Updatable, Cloneable {
    @NotEmpty
    @JsonProperty("groupName")
    private String group_name;

    @JsonIgnore
    private Long last_update;

    @JsonProperty("chatroom")
    private String chatroom;

    @JsonProperty("watchRecipients")
    private String watch_recipients;

    @JsonProperty("emailRecipients")
    private String email_recipients;

    @JsonProperty("pagerRecipients")
    private String pager_recipients;

    @JsonProperty("launchLatencyTh")
    private Integer launch_latency_th;

    @JsonProperty("healthcheckState")
    private Boolean healthcheck_state;

    @JsonProperty("healthcheckPeriod")
    private Long healthcheck_period;

    @JsonProperty("lifecycleState")
    private Boolean lifecycle_state;

    @JsonProperty("lifecycleTimeout")
    private Long lifecycle_timeout;

    @JsonProperty("lifecycleNotifications")
    private Boolean lifecycle_notifications;

    // the following two fields are used for backward compatible.
    @JsonIgnore
    private String launch_config_id;

    @JsonProperty("subnets")
    private String subnets;


    public String getGroup_name() { return group_name; }

    public void setGroup_name(String group_name) { this.group_name = group_name;}

    public Long getLast_update() { return last_update; }

    public void setLast_update(Long last_update) { this.last_update = last_update; }

    public String getChatroom() { return chatroom; }

    public void setChatroom(String chatroom) { this.chatroom = chatroom; }

    public String getWatch_recipients() { return watch_recipients; }

    public void setWatch_recipients(String watch_recipients) { this.watch_recipients = watch_recipients; }

    public String getEmail_recipients() { return email_recipients; }

    public void setEmail_recipients(String email_recipients) { this.email_recipients = email_recipients; }

    public String getPager_recipients() { return pager_recipients; }

    public void setPager_recipients(String pager_recipients) { this.pager_recipients = pager_recipients; }

    public Integer getLaunch_latency_th() { return launch_latency_th; }

    public void setLaunch_latency_th(Integer launch_latency_th) { this.launch_latency_th = launch_latency_th; }

    public Boolean getHealthcheck_state() { return healthcheck_state; }

    public void setHealthcheck_state(Boolean healthcheck_state) { this.healthcheck_state = healthcheck_state; }

    public Long getHealthcheck_period() { return healthcheck_period; }

    public void setHealthcheck_period(Long healthcheck_period) { this.healthcheck_period = healthcheck_period; }

    public Boolean getLifecycle_state() { return lifecycle_state; }

    public void setLifecycle_state(Boolean lifecycle_state) { this.lifecycle_state = lifecycle_state; }

    public Long getLifecycle_timeout() { return lifecycle_timeout; }

    public void setLifecycle_timeout(Long lifecycle_timeout) { this.lifecycle_timeout = lifecycle_timeout; }

    public Boolean getLifecycle_notifications() { return lifecycle_notifications; }

    public void setLifecycle_notifications(Boolean lifecycle_notifications) { this.lifecycle_notifications = lifecycle_notifications; }

    public String getLaunch_config_id() { return launch_config_id; }

    public void setLaunch_config_id(String launch_config_id) { this.launch_config_id = launch_config_id; }

    public String getSubnets() { return subnets; }

    public void setSubnets(String subnets) { this.subnets = subnets; }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("group_name", group_name);
        clause.addColumn("last_update", last_update);
        clause.addColumn("chatroom", chatroom);
        clause.addColumn("watch_recipients", watch_recipients);
        clause.addColumn("email_recipients", email_recipients);
        clause.addColumn("pager_recipients", pager_recipients);
        clause.addColumn("launch_latency_th", launch_latency_th);
        clause.addColumn("healthcheck_state", healthcheck_state);
        clause.addColumn("healthcheck_period", healthcheck_period);
        clause.addColumn("lifecycle_state", lifecycle_state);
        clause.addColumn("lifecycle_timeout", lifecycle_timeout);
        clause.addColumn("lifecycle_notifications", lifecycle_notifications);
        clause.addColumn("launch_config_id", launch_config_id);
        clause.addColumn("subnets", subnets);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
        "group_name=VALUES(group_name)," +
        "last_update=VALUES(last_update)," +
        "chatroom=VALUES(chatroom)," +
        "watch_recipients=VALUES(watch_recipients)," +
        "email_recipients=VALUES(email_recipients)," +
        "pager_recipients=VALUES(pager_recipients)," +
        "launch_latency_th=VALUES(launch_latency_th)," +
        "healthcheck_state=VALUES(healthcheck_state)," +
        "healthcheck_period=VALUES(healthcheck_period)," +
        "lifecycle_state=VALUES(lifecycle_state)," +
        "lifecycle_timeout=VALUES(lifecycle_timeout)," +
        "lifecycle_notifications=VALUES(lifecycle_notifications)," +
        "launch_config_id=VALUES(launch_config_id)," +
        "subnets=VALUES(subnets)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        GroupBean groupBean = (GroupBean)super.clone();
        groupBean.setGroup_name(group_name);
        groupBean.setLast_update(last_update);
        groupBean.setChatroom(chatroom);
        groupBean.setWatch_recipients(watch_recipients);
        groupBean.setEmail_recipients(email_recipients);
        groupBean.setPager_recipients(pager_recipients);
        groupBean.setLaunch_latency_th(launch_latency_th);
        groupBean.setHealthcheck_state(healthcheck_state);
        groupBean.setHealthcheck_period(healthcheck_period);
        groupBean.setLifecycle_state(lifecycle_state);
        groupBean.setLifecycle_timeout(lifecycle_timeout);
        groupBean.setLifecycle_notifications(lifecycle_notifications);
        groupBean.setLaunch_config_id(launch_config_id);
        groupBean.setSubnets(subnets);
        return groupBean;
    }
}
