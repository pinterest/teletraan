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
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/capacity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvCapacitys {
    public enum CapacityType {
        GROUP, HOST
    }

    private static final Logger LOG = LoggerFactory.getLogger(EnvCapacitys.class);
    private EnvironHandler environHandler;
    private EnvironDAO environDAO;
    private GroupDAO groupDAO;
    private Authorizer authorizer;

    @Context
    UriInfo uriInfo;

    public EnvCapacitys(TeletraanServiceContext context) {
        environHandler = new EnvironHandler(context);
        environDAO = context.getEnvironDAO();
        groupDAO = context.getGroupDAO();
        authorizer = context.getAuthorizer();
    }

    @GET
    public List<String> get(@PathParam("envName") String envName,
        @PathParam("stageName") String stageName,
        @QueryParam("capacityType") Optional<CapacityType> capacityType) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        if (capacityType.or(CapacityType.GROUP) == CapacityType.GROUP) {
            return groupDAO.getCapacityGroups(envBean.getEnv_id());
        } else {
            return groupDAO.getCapacityHosts(envBean.getEnv_id());
        }
    }

    @PUT
    public void update(@PathParam("envName") String envName,
        @PathParam("stageName") String stageName,
        @QueryParam("capacityType") Optional<CapacityType> capacityType,
        @NotEmpty List<String> names, @Context SecurityContext sc) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        if (capacityType.or(CapacityType.GROUP) == CapacityType.GROUP) {
            environHandler.updateGroups(envBean, names, operator);
        } else {
            environHandler.updateHosts(envBean, names, operator);
        }
        LOG.info("Successfully updated env {}/{} capacity config as {} by {}.",
            envName, stageName, names, operator);
    }

    @POST
    public void add(@PathParam("envName") String envName,
        @PathParam("stageName") String stageName,
        @QueryParam("capacityType") Optional<CapacityType> capacityType,
        @NotEmpty String name, @Context SecurityContext sc) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        if (capacityType.or(CapacityType.GROUP) == CapacityType.GROUP) {
            groupDAO.addGroupCapacity(envBean.getEnv_id(), name);
        } else {
            groupDAO.addHostCapacity(envBean.getEnv_id(), name);
        }
        LOG.info("Successfully added {} to env {}/{} capacity config by {}.",
            name, envName, stageName, operator);
    }

    @DELETE
    public void delete(@PathParam("envName") String envName,
        @PathParam("stageName") String stageName,
        @QueryParam("capacityType") Optional<CapacityType> capacityType,
        @NotEmpty String name, @Context SecurityContext sc) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        if (capacityType.or(CapacityType.GROUP) == CapacityType.GROUP) {
            groupDAO.removeGroupCapacity(envBean.getEnv_id(), name);
        } else {
            groupDAO.removeHostCapacity(envBean.getEnv_id(), name);
        }
        LOG.info("Successfully deleted {} from env {}/{} capacity config by {}.",
            name, envName, stageName, operator);
    }
}
