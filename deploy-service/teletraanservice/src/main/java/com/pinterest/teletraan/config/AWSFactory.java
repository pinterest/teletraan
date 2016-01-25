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
package com.pinterest.teletraan.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.arcee.autoscaling.AwsAlarmManager;
import com.pinterest.arcee.autoscaling.AwsAutoScaleGroupManager;
import com.pinterest.arcee.aws.AwsConfigManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

public class AWSFactory {
    private static Logger LOG = LoggerFactory.getLogger(AWSFactory.class);
    private String id;

    @NotNull
    @JsonProperty
    private String key;


    @JsonProperty
    private String vpc_id;

    @JsonProperty
    private String owner_id;

    @JsonProperty
    private String sqs_arn;

    @JsonProperty
    private String sns_arn;

    @JsonProperty
    private String role_arn;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }


    public AwsAutoScaleGroupManager buildAwsAutoScalingManager() {
        AmazonAutoScalingClient aasClient;
        if (StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(key)) {
            AWSCredentials myCredentials = new BasicAWSCredentials(id, key);
            aasClient = new AmazonAutoScalingClient(myCredentials);
        } else {
            LOG.info("AWS credential is missing for creating auto scaling client. Assuming to use IAM role for authentication.");
            aasClient = new AmazonAutoScalingClient();
        }
        return new AwsAutoScaleGroupManager(sns_arn, role_arn, aasClient);
    }

    public AwsAlarmManager buildAwsAlarmManager() {
        AmazonCloudWatchClient cloudWatcherClient;
        if (StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(key)) {
            AWSCredentials myCredentials = new BasicAWSCredentials(id, key);
            cloudWatcherClient = new AmazonCloudWatchClient(myCredentials);
        } else {
            LOG.info("AWS credential is missing for creating cloudwatch client. Assuming to use IAM role for authentication.");
            cloudWatcherClient = new AmazonCloudWatchClient();
        }
        return new AwsAlarmManager(cloudWatcherClient);
    }

    public AmazonEC2Client buildEC2Client() {
        if (StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(key)) {
            AWSCredentials myCredentials = new BasicAWSCredentials(id, key);
            return new AmazonEC2Client(myCredentials);
        } else {
            LOG.info("AWS credential is missing for creating ec2 client. Assuming to use IAM role for authentication.");
            return new AmazonEC2Client();
        }
    }

    public AwsConfigManager buildAwsConfigManager() {
        AwsConfigManager awsConfigManager = new AwsConfigManager();
        awsConfigManager.setId(id);
        awsConfigManager.setKey(key);
        awsConfigManager.setVpcId(vpc_id);
        awsConfigManager.setOwnerId(owner_id);
        awsConfigManager.setSqsArn(sqs_arn);
        awsConfigManager.setSnsArn(sns_arn);
        awsConfigManager.setRoleArn(role_arn);
        return awsConfigManager;
    }
}
