/*
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pinterest.clusterservice.aws;

import com.pinterest.arcee.aws.AwsConfigManager;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Subnet;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;


public class AwsManagerImpl implements AwsManager {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AwsManager.class);
    private AmazonEC2Client ec2Client;

    public AwsManagerImpl(AwsConfigManager configManager) {
        if (StringUtils.isNotEmpty(configManager.getId()) && StringUtils.isNotEmpty(configManager.getKey())) {
            AWSCredentials awsCredentials = new BasicAWSCredentials(configManager.getId(), configManager.getKey());
            this.ec2Client = new AmazonEC2Client(awsCredentials);
        } else {
            LOG.debug("AWS credential is missing for creating AWS client. Assuming to use role for authentication.");
            this.ec2Client = new AmazonEC2Client();
        }
    }

    @Override
    public int getAvailableCapacityInSubnet(String subnetId) throws Exception {
        DescribeSubnetsRequest subnetsRequest = new DescribeSubnetsRequest();
        subnetsRequest.setSubnetIds(Arrays.asList(subnetId));
        DescribeSubnetsResult subnetsResult = ec2Client.describeSubnets(subnetsRequest);
        List<Subnet> subnets = subnetsResult.getSubnets();
        if (subnets.isEmpty()) {
            return 0;
        }

        Subnet subnet = subnets.get(0);
        return subnet.getAvailableIpAddressCount();
    }
}
