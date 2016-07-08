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
package com.pinterest.arcee.lease;

import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.dao.LeaseDAO;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.dao.UtilDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;


public class InstanceLeaseManager implements LeaseDAO  {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceLeaseManager.class);
    private AutoScalingManager autoScalingManager;
    private UtilDAO utilDAO;
    private static final String LOCK_NAME = "%s_INSTANCE_LENDING";

    public InstanceLeaseManager(ServiceContext context) {
        autoScalingManager = context.getAutoScalingManager();
        utilDAO = context.getUtilDAO();
    }

    @Override
    public void lendInstances(String clusterName, int count) throws Exception {
        String groupName = String.format("%s-lending", clusterName);
        String lockName = String.format(LOCK_NAME, groupName);

        Connection connection = utilDAO.getLock(lockName);
        if (connection == null) {
            return;
        }
        try {
            AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(groupName);
            AwsVmBean updatedVmBean = new AwsVmBean();
            int currentSize = Math.min(awsVmBean.getMaxSize(), awsVmBean.getCurSize() + count);
            updatedVmBean.setCurSize(currentSize);
            autoScalingManager.updateAutoScalingGroup(groupName, updatedVmBean);
        } finally {
            utilDAO.releaseLock(lockName, connection);
        }
    }

    @Override
    public void returnInstances(String clusterName, int count) throws Exception {
        String groupName = String.format("%s-lending", clusterName);
        AwsVmBean awsVmBean = autoScalingManager.getAutoScalingGroupInfo(groupName);
        AwsVmBean updatedVmBean = new AwsVmBean();
        int currSize = Math.max(awsVmBean.getMinSize(), awsVmBean.getCurSize() - count);
        updatedVmBean.setCurSize(currSize);
        autoScalingManager.updateAutoScalingGroup(groupName, updatedVmBean);
    }
}
