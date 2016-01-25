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


import com.google.common.base.Optional;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.HealthCheckBean;
import com.pinterest.arcee.bean.HealthCheckErrorBean;
import com.pinterest.arcee.handler.GroupHandler;
import com.pinterest.arcee.handler.HealthCheckHandler;
import com.pinterest.arcee.handler.ProvisionHandler;
import com.pinterest.deployservice.bean.ConfigHistoryBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/v1/groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Groups {
    public enum ActionType {
        ROLLBACK
    }

    private static final Logger LOG = LoggerFactory.getLogger(Groups.class);
    private final static int DEFAULT_SIZE = 100;
    private GroupHandler groupHandler;
    private ConfigHistoryHandler configHistoryHandler;
    private ProvisionHandler provisionHandler;
    private EnvironDAO environDAO;
    private HealthCheckHandler healthCheckHandler;
    private final Authorizer authorizer;

    public Groups(TeletraanServiceContext context) {
        groupHandler = new GroupHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
        provisionHandler = new ProvisionHandler(context);
        healthCheckHandler = new HealthCheckHandler(context);
        environDAO = context.getEnvironDAO();
        authorizer = context.getAuthorizer();
    }

    @GET
    @Path("/names")
    public List<String> getGroupNames(@QueryParam("start") Optional<Integer> start,
        @QueryParam("size") Optional<Integer> size) throws Exception {
        return groupHandler.getEnvGroupNames(start.or(1), size.or(DEFAULT_SIZE));
    }

    @POST
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}")
    public void createGroup(@Context SecurityContext sc,
        @PathParam("groupName") String groupName, GroupBean groupBean) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.createGroup(groupName, groupBean);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_GENERAL, groupBean, operator);
        LOG.info("Successfully created group {} with config {} by {}", groupName, groupBean, operator);
    }

    @PUT
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}")
    public void updateGroupLaunchConfig(@Context SecurityContext sc,
        @PathParam("groupName") String groupName, GroupBean groupBean) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.updateLaunchConfig(groupName, groupBean);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_GENERAL, groupBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_GROUP, groupName, Constants.TYPE_ASG_GENERAL, operator);
        LOG.info("Successfully updated group {} with config {} by {}", groupName, groupBean, operator);
    }

    @GET
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}")
    public GroupBean get(@PathParam("groupName") String groupName) throws Exception {
        return groupHandler.getLaunchConfig(groupName);
    }

    @GET
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/instances")
    public List<HostBean> getHostsByGroup(@PathParam("groupName") String groupName) throws Exception {
        return groupHandler.getHostsByGroupName(groupName);
    }

    @PUT
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/instances")
    public List<String> launchNewInstances(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        @QueryParam("instanceCount") Optional<Integer> instanceCnt,
        @NotEmpty @QueryParam("subnet") String subnet) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        List<String> instanceIds = provisionHandler.launchNewInstances(groupName, instanceCnt.or(1), subnet, operator);
        if (!instanceIds.isEmpty()) {
            String configChange = String.format("%s", instanceIds);
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_HOST_LAUNCH, configChange, operator);
        }
        return instanceIds;
    }

    @DELETE
    @Path("/instances/{hostId: [a-zA-Z0-9\\-_]+}")
    public void terminateInstance(@Context SecurityContext sc,
        @PathParam("hostId") String hostId,
        @QueryParam("decreaseSize") Boolean decreaseSize,
        @QueryParam("groupName") String groupName) throws Exception {
        // TODO we need env name or group for this one!
        // Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        List<String> groupNames = provisionHandler.terminateHost(hostId, decreaseSize, operator);
        String configChange = String.format("Instance Id : %s", hostId);
        for (String name : groupNames) {
            configHistoryHandler.updateConfigHistory(name, Constants.TYPE_HOST_TERMINATE, configChange, operator);
        }
    }

    @GET
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/configs/history")
    public List<ConfigHistoryBean> getConfigHistory(@PathParam("groupName") String groupName,
        @QueryParam("pageIndex") Optional<Integer> pageIndex,
        @QueryParam("pageSize") Optional<Integer> pageSize) throws Exception {
        return configHistoryHandler.getConfigHistoryByName(groupName, pageIndex.or(1), pageSize.or(DEFAULT_SIZE));
    }

    @PUT
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/configs/actions")
    public void rollbackConfig(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        @NotEmpty @QueryParam("actionType") ActionType actionType,
        @QueryParam("changeId") String changeId) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        switch (actionType) {
            case ROLLBACK:
                configHistoryHandler.rollbackConfig(Constants.CONFIG_TYPE_GROUP, changeId, operator);
                LOG.info("{} rollbacked group {} config to config id = {}", operator, groupName, changeId);
                break;
            default:
                throw new TeletaanInternalException(Response.Status.BAD_REQUEST, "No action found.");
        }
    }

    @GET
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/healthchecks")
    public List<HealthCheckBean> getHealthChecks(@PathParam("groupName") String groupName,
                                                 @QueryParam("pageIndex") Optional<Integer> pageIndex,
                                                 @QueryParam("pageSize") Optional<Integer> pageSize) throws Exception {
        return healthCheckHandler.getHealthChecksByGroup(groupName, pageIndex.or(1), pageSize.or(DEFAULT_SIZE));
    }

    @GET
    @Path("/healthchecks/{id: [a-zA-Z0-9\\-_]+}")
    public HealthCheckBean getHealthCheck(@Context SecurityContext sc, @PathParam("id") String id) throws Exception {
        return healthCheckHandler.getHealthCheckById(id);
    }

    @GET
    @Path("/healthchecks/errors/{id: [a-zA-Z0-9\\-_]+}")
    public HealthCheckErrorBean getHealthCheckError(@Context SecurityContext sc, @PathParam("id") String id) throws Exception {
        return healthCheckHandler.getHealthCheckErrorById(id);
    }

    @POST
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/healthcheck")
    public void createHealthCheck(@Context SecurityContext sc,
                                  @PathParam("groupName") String groupName,
                                  HealthCheckBean healthCheckBean) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();

        healthCheckBean.setGroup_name(groupName);
        List<String> ids = healthCheckHandler.createHealthCheck(healthCheckBean);
        String results = ids.toString();
        if (ids.isEmpty()) {
            results = "Health check isn't enabled";
        }
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_HELATHCHECK_MANAUALLY, results, operator);
        LOG.info("{} have added to group {} by {}", results, groupName, operator);
    }

}
