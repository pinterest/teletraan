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
package com.pinterest.teletraan.resource;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static EnvironBean getEnvStage(EnvironDAO environDAO, String envName,
        String stageName) throws Exception {
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        if (environBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND,
                String.format("Environment %s/%s does not exist.", envName, stageName));
        }
        return environBean;
    }

    public static EnvironBean getEnvStage(EnvironDAO environDAO, String envId) throws Exception {
        EnvironBean environBean = environDAO.getById(envId);
        if (environBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND,
                String.format("Environment %s does not exist.", envId));
        }
        return environBean;
    }

    private static void authorizeEnvirons(List<EnvironBean> environBeans,
        SecurityContext sc, Authorizer authorizer, Role role) throws Exception {
        for (EnvironBean environBean : environBeans) {
            try {
                Resource resource = new Resource(environBean.getEnv_name(), Resource.Type.ENV);
                authorizer.authorize(sc, resource, role);
                return;
            } catch (Exception e) {
                // Not authorized, for whatever reason
                LOG.info("Failed to authorize {} for resource {} and role {}",
                    sc.getUserPrincipal().getName(), environBean.getEnv_name(), role);
            }
        }
        throw new TeletaanInternalException(Response.Status.FORBIDDEN, "Not authorized!");
    }

    public static void authorizeGroup(EnvironDAO environDAO, String groupName,
        SecurityContext sc, Authorizer authorizer, Role role) throws Exception {
        List<EnvironBean> environBeans = environDAO.getEnvsByGroups(Arrays.asList(groupName));
        if (CollectionUtils.isEmpty(environBeans)) {
            // For groups not associate with environ yet, just pass it
            LOG.warn("Group {} is not managed by Teletraan yet, authorize the action for now",
                groupName);
            return;
        }
        authorizeEnvirons(environBeans, sc, authorizer, role);
    }

    public static void authorizeHost(EnvironDAO environDAO, String hostName,
        SecurityContext sc, Authorizer authorizer, Role role) throws Exception {
        List<EnvironBean> environBeans = environDAO.getEnvsByHost(hostName);
        if (CollectionUtils.isEmpty(environBeans)) {
            // For groups not associate with environ yet, just pass it
            LOG.warn("Host {} is not managed by Teletraan yet, authorize the action for now",
                hostName);
            return;
        }
        authorizeEnvirons(environBeans, sc, authorizer, role);
    }

    public static DeployBean getDeploy(DeployDAO deployDAO, String deployId) throws Exception {
        DeployBean deployBean = deployDAO.getById(deployId);
        if (deployBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND,
                String.format("Deploy %s does not exist.", deployId));
        }
        return deployBean;
    }

    public static void trimMapValues(Map<String, String> configs) throws Exception {

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            entry.setValue(entry.getValue().trim());
        }
    }
}
