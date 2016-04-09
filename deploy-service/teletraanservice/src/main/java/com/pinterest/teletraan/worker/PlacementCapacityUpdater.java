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

package com.pinterest.teletraan.worker;


import com.pinterest.clusterservice.aws.AwsManager;
import com.pinterest.clusterservice.bean.CloudProvider;
import com.pinterest.clusterservice.bean.PlacementBean;
import com.pinterest.clusterservice.dao.PlacementDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.dao.UtilDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collection;

public class PlacementCapacityUpdater implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PlacementCapacityUpdater.class);
    private final PlacementDAO placementDAO;
    private final UtilDAO utilDAO;
    private final AwsManager awsManager;

    public PlacementCapacityUpdater(ServiceContext serviceContext) {
        placementDAO = serviceContext.getPlacementDAO();
        utilDAO = serviceContext.getUtilDAO();
        awsManager = serviceContext.getAwsManager();
    }

    private void processBatch() throws Exception {
        Collection<PlacementBean> placementBeans = placementDAO.getByProvider(CloudProvider.AWS.toString());
        for (PlacementBean bean : placementBeans) {
            LOG.info("Start to process placement {} ", bean.toString());
            String placementId = bean.getId();
            String lockName = String.format("PLACEMENT-%s", placementId);
            Connection connection = utilDAO.getLock(lockName);
            if (connection != null) {
                try {
                    int capacity = awsManager.getAvailableCapacityInSubnet(bean.getProvider_name());
                    if (capacity != bean.getCapacity()) {
                        PlacementBean updateBean = new PlacementBean();
                        updateBean.setCapacity(capacity);
                        placementDAO.updateById(placementId, updateBean);
                    }
                } catch (Exception ex) {
                    LOG.error("Failed to process placement {}", bean.toString(), ex);
                } finally {
                    utilDAO.releaseLock(lockName, connection);
                    Thread.sleep(500);
                }
            } else {
                LOG.warn(String.format("Failed to get lock: %s", lockName));
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Start to run PlacementCapacityUpdater");
            processBatch();
        } catch (Throwable t) {
            LOG.error("Failed to run PlacementCapacityUpdater");
        }
    }
}
