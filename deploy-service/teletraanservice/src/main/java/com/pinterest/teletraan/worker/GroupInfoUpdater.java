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
package com.pinterest.teletraan.worker;

import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.bean.AutoScalingGroupBean;
import com.pinterest.arcee.bean.SpotAutoScalingBean;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.deployservice.dao.HostDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;


public class GroupInfoUpdater implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GroupInfoUpdater.class);
    private static final String COUNTER_NAME = "autoscaling.%s.size";
    private GroupInfoDAO groupInfoDAO;
    private HostDAO hostDAO;
    private HashMap<String, String> tags;
    private MetricSource metricSource;
    private AutoScalingManager autoScalingManager;
    private SpotAutoScalingDAO spotAutoScalingDAO;

    public GroupInfoUpdater(ServiceContext context) {
        groupInfoDAO = context.getGroupInfoDAO();
        hostDAO = context.getHostDAO();
        metricSource = context.getMetricSource();
        autoScalingManager = context.getAutoScalingManager();
        spotAutoScalingDAO = context.getSpotAutoScalingDAO();
        tags = new HashMap<>();
        try {
            tags.put("host", InetAddress.getLocalHost().getHostName());
        } catch (Exception t) {
            LOG.error("Failed to get local host name");
        }
    }

    private void sendGroupMetrics(String groupName) throws Exception {
        Long groupSize = hostDAO.getGroupSize(groupName);

        SpotAutoScalingBean spotAutoScalingBean = spotAutoScalingDAO.getAutoScalingGroupsByCluster(groupName);
        Long currentTime = System.currentTimeMillis();
        if (spotAutoScalingBean != null) {
            String spotAutoScalingName = String.format("%s-spot", groupName);
            AutoScalingGroupBean autoScalingGroupBean = autoScalingManager
                .getAutoScalingGroupInfoByName(spotAutoScalingName);
            if (!autoScalingGroupBean.getStatus().equals(ASGStatus.UNKNOWN)) {
                Integer spotInstanceCount = autoScalingGroupBean.getInstances().size();
                groupSize = groupSize - spotInstanceCount;
                metricSource.export(String.format(COUNTER_NAME, spotAutoScalingName), tags,
                                    spotInstanceCount.doubleValue(), currentTime);
            }
        }
        metricSource.export(String.format(COUNTER_NAME, groupName), tags, groupSize.doubleValue(), currentTime);
    }

    public void processBatch() throws Exception {
        List<String> groups = groupInfoDAO.getExistingGroups(1, 1000);
        Collections.shuffle(groups);
        for (String group : groups) {
            try {
                LOG.info("Start to send metrics to tsd for group: {}", group);
                sendGroupMetrics(group);
            } catch (Exception ex) {
                LOG.error("Failed to send group {} to tsd.", group, ex);
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start to run GroupInfoUpdater.");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Faile to run GroupInfoUpdater", t);
        }
    }
}
