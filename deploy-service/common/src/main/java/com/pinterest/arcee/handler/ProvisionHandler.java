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
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.dao.GroupInfoDAO;
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
    private GroupInfoDAO groupInfoDAO;
    private AutoScalingManager asgDAO;
    private GroupHandler groupHandler;

    public ProvisionHandler(ServiceContext serviceContext) {
        hostDAO = serviceContext.getHostDAO();
        hostInfoDAO = serviceContext.getHostInfoDAO();
        groupInfoDAO = serviceContext.getGroupInfoDAO();
        asgDAO = serviceContext.getAutoScalingManager();
        groupHandler = new GroupHandler(serviceContext);
    }

    public List<String> launchNewInstances(String groupName, int instanceCnt, String subnet, String operator) throws Exception {
        GroupBean groupBean = groupHandler.getGroupInfoByClusterName(groupName);
        if (groupBean != null) {
            if (groupBean.getAsg_status() == ASGStatus.ENABLED) {  // AutoScaling is enabled, increase the ASG capacity
                LOG.info(String.format("Launch %d EC2 instances in AutoScalingGroup %s", instanceCnt, groupName));
                asgDAO.increaseGroupCapacity(groupName, instanceCnt);
                return new ArrayList<>();
            } else if (groupBean.getAsg_status() == ASGStatus.DISABLED) { // AutoScaling is disabled, do nothing
                LOG.info(String.format("AutoScalingGroup %s is disabled. Prohibit from launching instances", groupName));
                return new ArrayList<>();
            } else {   // No ASG for this group, directly launch EC2 instances
                LOG.info(String.format("Launch %d EC2 instances in group %s and subnet %s", instanceCnt, groupName, subnet));
                List<HostBean> hosts = hostInfoDAO.launchEC2Instances(groupBean, instanceCnt, subnet);
                List<String> hostIds = new ArrayList<>();
                for (HostBean host : hosts) {
                    LOG.debug(String.format("An new instance %s has been launched in group %s", host.getHost_id(), groupName));
                    hostIds.add(host.getHost_id());
                    hostDAO.insert(host);
                }
                return hostIds;
            }
        }
        return new ArrayList<>();
    }
}
