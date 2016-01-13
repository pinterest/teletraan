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
 * launch_config_id     VARCHAR(81),
 * last_update          BIGINT(20),     NOT NULL,
 * chatroom             VARCHAR(64),
 * email_recipients     VARCHAR(1024),
 * watch_recipients     VARCHAR(1024),
 * launch_latency_th    INT             NOT NULL DEFAULT 600,
 * instance_type        VARCHAR(64),
 * image_id             VARCHAR(128),
 * security_group       VARCHAR(128),
 * subnets              VARCHAR(128),
 * user_data            TEXT,
 * iam_role             VARCHAR(64),
 * assign_public_ip     TINYINT(1)      NOT NULL DEFAULT 0,
 * asg_satus            VARCHAR(64),
 * healthcheck_state    TINYINT(1)      NOT NULL DEFAULT 0,
 * healthcheck_period   BIGINT          NOT NULL DEFAULT 3600,
 * PRIMARY KEY (group_name)
 * );
 */
public class GroupBean implements Updatable, Cloneable {
    @NotEmpty
    @JsonProperty("groupName")
    private String group_name;

    @JsonIgnore
    private String launch_config_id;

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

    @NotEmpty
    @JsonProperty("instanceType")
    private String instance_type;

    @NotEmpty
    @JsonProperty("imageId")
    private String image_id;

    @NotEmpty
    @JsonProperty("securityGroup")
    private String security_group;

    @NotNull
    @JsonProperty("userData")
    private String user_data;

    @NotNull
    @JsonProperty("subnets")
    private String subnets;

    @NotNull
    @JsonProperty("iamRole")
    private String iam_role;

    @NotNull
    @JsonProperty("assignPublicIp")
    private Boolean assign_public_ip;

    @JsonProperty("asgStatus")
    private ASGStatus asg_status;

    @JsonProperty("healthcheckState")
    private Boolean healthcheck_state;

    @JsonProperty("healthcheckPeriod")
    private Long healthcheck_period;

    public String getGroup_name() { return group_name; }

    public void setGroup_name(String group_name) { this.group_name = group_name;}

    public String getLaunch_config_id() { return launch_config_id; }

    public void setLaunch_config_id(String launch_config_id) { this.launch_config_id = launch_config_id; }

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

    public String getInstance_type() { return instance_type; }

    public void setInstance_type(String instance_type) { this.instance_type = instance_type; }

    public String getImage_id() { return image_id; }

    public void setImage_id(String image_id) { this.image_id = image_id; }

    public String getSecurity_group() { return security_group; }

    public void setSecurity_group(String security_group) { this.security_group = security_group; }

    public String getUser_data() { return user_data; }

    public void setUser_data(String user_data) { this.user_data = user_data; }

    public String getSubnets() { return subnets; }

    public void setSubnets(String subnets) { this.subnets = subnets; }

    public String getIam_role() { return iam_role; }

    public void setIam_role(String iam_role) { this.iam_role = iam_role; }

    public Boolean getAssign_public_ip() { return assign_public_ip; }

    public void setAssign_public_ip(Boolean assign_public_ip) { this.assign_public_ip = assign_public_ip; }

    public ASGStatus getAsg_status() { return asg_status; }

    public void setAsg_status(ASGStatus asg_status) { this.asg_status = asg_status; }

    public Boolean getHealthcheck_state() { return healthcheck_state; }

    public void setHealthcheck_state(Boolean healthcheck_state) { this.healthcheck_state = healthcheck_state; }

    public Long getHealthcheck_period() { return healthcheck_period; }

    public void setHealthcheck_period(Long healthcheck_period) { this.healthcheck_period = healthcheck_period; }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("group_name", group_name);
        clause.addColumn("launch_config_id", launch_config_id);
        clause.addColumn("last_update", last_update);
        clause.addColumn("chatroom", chatroom);
        clause.addColumn("watch_recipients", watch_recipients);
        clause.addColumn("email_recipients", email_recipients);
        clause.addColumn("pager_recipients", pager_recipients);
        clause.addColumn("launch_latency_th", launch_latency_th);
        clause.addColumn("instance_type", instance_type);
        clause.addColumn("image_id", image_id);
        clause.addColumn("security_group", security_group);
        clause.addColumn("subnets", subnets);
        clause.addColumn("user_data", user_data);
        clause.addColumn("iam_role", iam_role);
        clause.addColumn("assign_public_ip", assign_public_ip);
        clause.addColumn("asg_status", asg_status);
        clause.addColumn("healthcheck_state", healthcheck_state);
        clause.addColumn("healthcheck_period", healthcheck_period);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        GroupBean groupBean = (GroupBean)super.clone();
        groupBean.setGroup_name(group_name);
        groupBean.setLaunch_config_id(launch_config_id);
        groupBean.setLast_update(last_update);
        groupBean.setChatroom(chatroom);
        groupBean.setWatch_recipients(watch_recipients);
        groupBean.setEmail_recipients(email_recipients);
        groupBean.setPager_recipients(pager_recipients);
        groupBean.setLaunch_latency_th(launch_latency_th);
        groupBean.setInstance_type(instance_type);
        groupBean.setImage_id(image_id);
        groupBean.setSecurity_group(security_group);
        groupBean.setUser_data(user_data);
        groupBean.setSubnets(subnets);
        groupBean.setIam_role(iam_role);
        groupBean.setAssign_public_ip(assign_public_ip);
        groupBean.setAsg_status(asg_status);
        groupBean.setHealthcheck_state(healthcheck_state);
        groupBean.setHealthcheck_period(healthcheck_period);
        return groupBean;
    }
}
