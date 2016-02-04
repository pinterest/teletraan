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
package com.pinterest.clusterservice.cm;

import com.pinterest.arcee.aws.AwsConfigManager;
import com.pinterest.clusterservice.bean.ClusterBean;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class AwsVMManager implements ClusterManager {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AwsVMManager.class);
    private AmazonAutoScalingClient aasClient;
    private String snsArn;
    private String roleArn;

    public AwsVMManager(AwsConfigManager configManager) {
        if (StringUtils.isNotEmpty(configManager.getId()) && StringUtils.isNotEmpty(configManager.getKey())) {
            AWSCredentials awsCredentials = new BasicAWSCredentials(configManager.getId(), configManager.getKey());
            this.aasClient = new AmazonAutoScalingClient(awsCredentials);
        } else {
            LOG.info("AWS credential is missing for creating AWS client. Assuming to use role for authentication.");
            this.aasClient = new AmazonAutoScalingClient();
        }

        this.snsArn = configManager.getSnsArn();
        this.roleArn = configManager.getRoleArn();
    }

    @Override
    public void createCluster(String clusterName, ClusterBean bean) throws Exception {

    }

    @Override
    public void updateCluster(String clusterName, ClusterBean bean) throws Exception {

    }

    @Override
    public ClusterBean getCluster(String clusterName) throws Exception {
        return new ClusterBean();
    }

    @Override
    public void deleteCluster(String clusterName) throws Exception {

    }

    @Override
    public void launchHosts(String clusterName, int num) throws Exception {

    }

    @Override
    public void terminateHosts(Collection<String> hostIds) throws Exception {

    }
}
