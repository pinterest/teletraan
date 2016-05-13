package com.pinterest.arcee.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.clusterservice.bean.AwsVmBean;


public class GroupInfoBean {
    @JsonProperty("groupInfo")
    private GroupBean groupBean;

    @JsonProperty("launchInfo")
    private AwsVmBean awsVmBean;

    public GroupBean getGroupBean() {
         return groupBean;
    }

    public void setGroupBean(GroupBean groupBean) { this.groupBean = groupBean; }

    public AwsVmBean getAwsVmBean() { return awsVmBean; }

    public void setAwsVmBean(AwsVmBean awsVmBean) { this.awsVmBean = awsVmBean; }

}
