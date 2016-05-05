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

package com.pinterest.clusterservice.handler;


import com.pinterest.clusterservice.bean.ClusterUpgradeEventBean;
import com.pinterest.clusterservice.bean.ClusterUpgradeEventState;
import com.pinterest.clusterservice.bean.ClusterUpgradeEventStatus;
import com.pinterest.clusterservice.dao.ClusterUpgradeEventDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.common.CommonUtils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class ClusterUpgradeEventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterUpgradeEventHandler.class);
    private final ClusterUpgradeEventDAO clusterUpgradeEventDAO;

    public ClusterUpgradeEventHandler(ServiceContext serviceContext) {
        this.clusterUpgradeEventDAO = serviceContext.getClusterUpgradeEventDAO();
    }

    public void createClusterUpgradeEvent(ClusterUpgradeEventBean eventBean) throws Exception {
        if (StringUtils.isEmpty(eventBean.getCluster_name()) || StringUtils.isEmpty(eventBean.getEnv_id())) {
            LOG.error("Empty cluster name or env id");
            throw new Exception("Empty cluster name or env id");
        }
        abortClusterUpgradeEvents(eventBean.getCluster_name());

        eventBean.setId(CommonUtils.getBase64UUID());
        eventBean.setState(ClusterUpgradeEventState.INIT);
        eventBean.setStatus(ClusterUpgradeEventStatus.UNKNOWN);
        long currTime = System.currentTimeMillis();
        eventBean.setStart_time(currTime);
        eventBean.setState_start_time(currTime);
        eventBean.setLast_worked_on(currTime);
        clusterUpgradeEventDAO.insertClusterUpgradeEvent(eventBean);
    }

    public void abortClusterUpgradeEvents(String clusterName) throws Exception {
        Collection<ClusterUpgradeEventBean> ongoingEvents = clusterUpgradeEventDAO.getOngoingEventsByCluster(clusterName);
        if (ongoingEvents.isEmpty()) {
            return;
        }

        for (ClusterUpgradeEventBean eventBean : ongoingEvents) {
            LOG.info(String.format("Abort cluster upgrade event %s for cluster %s", eventBean.getId(), eventBean.getCluster_name()));
            ClusterUpgradeEventBean updateBean = new ClusterUpgradeEventBean();
            if (eventBean.getState() == ClusterUpgradeEventState.COMPLETING) {
                updateBean.setStatus(ClusterUpgradeEventStatus.ABORT);
            } else {
                updateBean.setState(ClusterUpgradeEventState.COMPLETING);
                updateBean.setStatus(ClusterUpgradeEventStatus.ABORT);
            }
            updateBean.setLast_worked_on(System.currentTimeMillis());
            updateBean.setState_start_time(System.currentTimeMillis());
            clusterUpgradeEventDAO.updateById(eventBean.getId(), updateBean);
        }
    }
}
