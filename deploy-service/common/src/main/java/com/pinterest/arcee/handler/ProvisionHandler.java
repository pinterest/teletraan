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
package com.pinterest.arcee.handler;

import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.dao.HostDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class ProvisionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ProvisionHandler.class);
    private HostDAO hostDAO;
    private HostInfoDAO hostInfoDAO;
    private AutoScalingManager autoScalingManager;

    public ProvisionHandler(ServiceContext serviceContext) {
        hostDAO = serviceContext.getHostDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        autoScalingManager = serviceContext.getAutoScalingManager();
    }

    public Collection<String> launchHosts(String groupName, int instanceCnt, String subnet) throws Exception {
        AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(groupName);
        if (subnet == null) {
            LOG.info(String.format("Launch %d EC2 instances in AutoScalingGroup %s", instanceCnt, groupName));
            autoScalingManager.increaseGroupCapacity(groupName, instanceCnt);
            return new ArrayList<>();
        } else {   // No ASG for this group, directly launch EC2 instances
            LOG.info(String.format("Launch %d EC2 instances in group %s and subnet %s", instanceCnt,
                                   groupName, subnet));
            Collection<HostBean> hosts = hostInfoDAO.launchHosts(awsVmBean, instanceCnt, subnet);
            Collection<String> hostIds = new ArrayList<>();
            for (HostBean host : hosts) {
                LOG.debug(String.format("An new instance %s has been launched in group %s",
                                        host.getHost_id(), groupName));
                hostIds.add(host.getHost_id());
                hostDAO.insert(host);
            }
            return hostIds;
        }
    }
}
