/**
 * Copyright (c) 2025 Pinterest, Inc.
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
package com.pinterest.teletraan.handler;

import com.google.common.collect.ImmutableList;
import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.config.AuthorizationFactory;
import com.pinterest.teletraan.resource.EnvCapacities.CapacityType;
import com.pinterest.teletraan.resource.Utils;
import com.pinterest.teletraan.universal.security.TeletraanAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import java.security.Principal;
import java.util.*;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentHandler.class);

    private final AuthorizationFactory authorizationFactory;
    private final ConfigHistoryHandler configHistoryHandler;
    private final EnvironDAO environDAO;
    private final EnvironHandler environHandler;
    private final GroupDAO groupDAO;
    private final TeletraanServiceContext context;

    public EnvironmentHandler(TeletraanServiceContext context) {
        authorizationFactory = context.getAuthorizationFactory();
        configHistoryHandler = new ConfigHistoryHandler(context);
        environDAO = context.getEnvironDAO();
        environHandler = new EnvironHandler(context);
        groupDAO = context.getGroupDAO();
        this.context = context;
    }

    public void createCapacityForHostOrGroup(
            SecurityContext sc,
            String envName,
            String stageName,
            Optional<CapacityType> capacityType,
            String name,
            EnvironBean environBean)
            throws Exception {
        name = name.replace("\"", "");
        List<String> names = ImmutableList.of(name);

        authorize(
                environBean, sc.getUserPrincipal(), capacityType.orElse(CapacityType.GROUP), names);
        String operator = sc.getUserPrincipal().getName();
        if (capacityType.orElse(CapacityType.GROUP) == CapacityType.GROUP) {
            groupDAO.addGroupCapacity(environBean.getEnv_id(), name);
        } else {
            groupDAO.addHostCapacity(environBean.getEnv_id(), name);
        }
        LOG.info(
                "Successfully added {} to env {}/{} capacity config by {}.",
                name,
                envName,
                stageName,
                operator);
    }

    public void updateEnvironment(
            SecurityContext sc, String envName, String stageName, EnvironBean updateEnvironBean)
            throws Exception {
        EnvironBean originEnvironBean = Utils.getEnvStage(environDAO, envName, stageName);
        // treat null as false
        boolean originalIsSox =
                originEnvironBean.getIs_sox() != null && originEnvironBean.getIs_sox();

        if (updateEnvironBean.getIs_sox() == null) {
            updateEnvironBean.setIs_sox(originalIsSox);
        } else if (!updateEnvironBean.getIs_sox().equals(originalIsSox)) {
            throw new WebApplicationException(
                    "Modification of isSox flag is not allowed!", Response.Status.FORBIDDEN);
        }

        String operator = sc.getUserPrincipal().getName();
        try {
            updateEnvironBean.validate();
            stageTypeValidate(originEnvironBean, updateEnvironBean);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.toString(), Response.Status.BAD_REQUEST);
        }

        if (updateEnvironBean.getStage_type() == EnvType.DEV) {
            updateEnvironBean.setAllow_private_build(true);
        } else if (originEnvironBean.getStage_type() == EnvType.DEV) {
            updateEnvironBean.setAllow_private_build(false);
        }
        updateEnvironBean.setEnv_name(originEnvironBean.getEnv_name());
        updateEnvironBean.setStage_name(originEnvironBean.getStage_name());
        if (updateEnvironBean.getExternal_id() == null) {
            updateEnvironBean.setExternal_id(originEnvironBean.getExternal_id());
        }
        environHandler.updateStage(updateEnvironBean, operator);
        configHistoryHandler.updateConfigHistory(
                originEnvironBean.getEnv_id(),
                Constants.TYPE_ENV_GENERAL,
                updateEnvironBean,
                operator);
        configHistoryHandler.updateChangeFeed(
                Constants.CONFIG_TYPE_ENV,
                originEnvironBean.getEnv_id(),
                Constants.TYPE_ENV_GENERAL,
                operator,
                updateEnvironBean.getExternal_id());
        LOG.info(
                "Successfully updated env {}/{} with {} by {}.",
                envName,
                stageName,
                updateEnvironBean,
                operator);
    }

    private void stageTypeValidate(EnvironBean origBean, EnvironBean newBean) throws Exception {
        Map<EnvType, String> stageTypeCategory = new HashMap<>();
        stageTypeCategory.put(EnvType.DEFAULT, "PRODUCTION");
        stageTypeCategory.put(EnvType.PRODUCTION, "PRODUCTION");
        stageTypeCategory.put(EnvType.CONTROL, "PRODUCTION");
        stageTypeCategory.put(EnvType.CANARY, "PRODUCTION");
        stageTypeCategory.put(EnvType.STAGING, "NON-PRODUCTION");
        stageTypeCategory.put(EnvType.LATEST, "NON-PRODUCTION");
        stageTypeCategory.put(EnvType.DEV, "NON-PRODUCTION");

        if (origBean.getStage_type() == EnvType.DEFAULT && newBean.getStage_type() == null) {
            throw new IllegalArgumentException(
                    "Please update the Stage Type to a value other than DEFAULT.");
        } else if (newBean.getStage_type() == null) {
            // Request has no intention to change stage type, so set it to the current value
            // to avoid the default value being used.
            newBean.setStage_type(origBean.getStage_type());
        } else if (origBean.getStage_type() != EnvType.DEFAULT
                && origBean.getStage_type() != newBean.getStage_type()
                && stageTypeCategory.get(newBean.getStage_type()).equals("NON-PRODUCTION")
                && stageTypeCategory.get(origBean.getStage_type()).equals("PRODUCTION")) {
            throw new IllegalArgumentException(
                    "Modification of Production stage type (PRODUCTION, CANARY, CONTROL) is not allowed!");
        }
    }

    public void deleteCapacityForHostOrGroup(SecurityContext sc, String envName, String stageName)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();
        environHandler.deleteEnvStage(envName, stageName, operator);
        LOG.info("Successfully deleted env {}/{} by {}.", envName, stageName, operator);
    }

    public void authorize(
            EnvironBean targetEnvironBean,
            Principal principal,
            CapacityType capacityType,
            List<String> capacities) {
        if (isSidecarEnvironment(targetEnvironBean)) {
            // Allow sidecars to add capacity
            return;
        }

        if (!(principal instanceof TeletraanPrincipal)) {
            throw new UnsupportedOperationException("Only TeletraanPrincipal is allowed");
        }
        TeletraanPrincipal teletraanPrincipal = (TeletraanPrincipal) principal;
        TeletraanAuthorizer<TeletraanPrincipal> authorizer =
                authorizationFactory.createSecondaryAuthorizer(
                        context, teletraanPrincipal.getClass());

        HashSet<AuthZResource> resources = getCapacityMainEnvironments(capacityType, capacities);
        for (AuthZResource resource : resources) {
            if (!authorizer.authorize(
                    teletraanPrincipal, TeletraanPrincipalRole.Names.WRITE, resource, null)) {
                throw new ForbiddenException(
                        String.format(
                                "Principal %s is not allowed to modify capacity owned by env %s",
                                principal.getName(), resource.getName()));
            }
        }
    }

    private HashSet<AuthZResource> getCapacityMainEnvironments(
            CapacityType capacityType, List<String> capacities) throws WebApplicationException {
        HashSet<AuthZResource> resources = new HashSet<>();
        for (String capacity : capacities) {
            EnvironBean envBean;
            try {
                if (capacityType == CapacityType.GROUP) {
                    envBean = environDAO.getByCluster(capacity);
                } else {
                    envBean = environDAO.getMainEnvByHostName(capacity);
                }
            } catch (Exception e) {
                throw new InternalServerErrorException(e);
            }

            if (envBean != null) {
                resources.add(new AuthZResource(envBean.getEnv_name(), envBean.getStage_name()));
            } else {
                LOG.info(
                        "Failed to find main environment for capacity {}, skip authorization",
                        capacity);
            }
        }
        return resources;
    }

    private boolean isSidecarEnvironment(EnvironBean environBean) {
        return environBean.getSystem_priority() != null && environBean.getSystem_priority() > 0;
    }
}
