/*
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

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;

import com.google.common.base.Optional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/capacity")
@Api(tags = "Environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvCapacities {

  private static final Logger LOG = LoggerFactory.getLogger(EnvCapacities.class);

  private EnvironHandler environHandler;
  private ConfigHistoryHandler configHistoryHandler;
  private EnvironDAO environDAO;
  private GroupDAO groupDAO;
  private Authorizer authorizer;

  public EnvCapacities(@Context TeletraanServiceContext context) {
    environHandler = new EnvironHandler(context);
    configHistoryHandler = new ConfigHistoryHandler(context);
    environDAO = context.getEnvironDAO();
    groupDAO = context.getGroupDAO();
    authorizer = context.getAuthorizer();
  }

  @GET
  @ApiOperation(
      value = "Get the capacities for Group and hosts",
      notes = "Get the capacities for Group and hosts")
  public List<String> get(@PathParam("envName") String envName,
                          @PathParam("stageName") String stageName,
                          @QueryParam("capacityType") Optional<CapacityType> capacityType)
      throws Exception {
    EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
    if (capacityType.or(CapacityType.GROUP) == CapacityType.GROUP) {
      return groupDAO.getCapacityGroups(envBean.getEnv_id());
    } else {
      return groupDAO.getCapacityHosts(envBean.getEnv_id());
    }
  }

  @PUT
  @ApiOperation(
      value = "Update the capacities for Group and hosts",
      notes = "Update the capacities for Group and hosts")
  public void update(@PathParam("envName") String envName,
                     @PathParam("stageName") String stageName,
                     @QueryParam("capacityType") Optional<CapacityType> capacityType,
                     @NotNull List<String> names, @Context SecurityContext sc) throws Exception {
    EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
    authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
    String operator = sc.getUserPrincipal().getName();
    if (capacityType.or(CapacityType.GROUP) == CapacityType.GROUP) {
      environHandler.updateGroups(envBean, names, operator);
      configHistoryHandler
          .updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_GROUP_CAPACITY, names,
              operator);
      configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(),
          Constants.TYPE_ENV_GROUP_CAPACITY, operator, envBean.getExternal_id());
    } else {
      environHandler.updateHosts(envBean, names, operator);
      configHistoryHandler
          .updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_HOST_CAPACITY, names,
              operator);
      configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(),
          Constants.TYPE_ENV_HOST_CAPACITY, operator, envBean.getExternal_id());
    }
    LOG.info("Successfully updated env {}/{} capacity config as {} by {}.",
        envName, stageName, names, operator);
  }

  @POST
  @ApiOperation(
      value = "Create the capacities for Group and hosts",
      notes = "Create the capacities for Group and hosts")
  public void add(@PathParam("envName") String envName,
                  @PathParam("stageName") String stageName,
                  @QueryParam("capacityType") Optional<CapacityType> capacityType,
                  @NotEmpty String name, @Context SecurityContext sc) throws Exception {
    EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
    authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
    String operator = sc.getUserPrincipal().getName();
    name = name.replaceAll("\"", "");
    if (capacityType.or(CapacityType.GROUP) == CapacityType.GROUP) {
      groupDAO.addGroupCapacity(envBean.getEnv_id(), name);
    } else {
      groupDAO.addHostCapacity(envBean.getEnv_id(), name);
    }
    LOG.info("Successfully added {} to env {}/{} capacity config by {}.",
        name, envName, stageName, operator);
  }

  @DELETE
  @ApiOperation(
      value = "Delete the capacities for Group and hosts",
      notes = "Delete the capacities for Group and hosts")
  public void delete(@PathParam("envName") String envName,
                     @PathParam("stageName") String stageName,
                     @QueryParam("capacityType") Optional<CapacityType> capacityType,
                     @NotEmpty String name, @Context SecurityContext sc) throws Exception {
    EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
    authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
    String operator = sc.getUserPrincipal().getName();
    name = name.replaceAll("\"", "");
    if (capacityType.or(CapacityType.GROUP) == CapacityType.GROUP) {
      LOG.info("Delete group {} from environment {} stage {} capacity", name,
          envName, stageName);
      groupDAO.removeGroupCapacity(envBean.getEnv_id(), name);
      if (StringUtils.equalsIgnoreCase(envBean.getCluster_name(), name)) {
        LOG.info("Delete cluster {} from environment {} stage {}", name, envName, stageName);
        //The group is set to be the cluster
        environDAO.deleteCluster(envName, stageName);
      }
    } else {
      LOG.info("Delete host {} from environment {} stage {} capacity", name,
          envName, stageName);
      groupDAO.removeHostCapacity(envBean.getEnv_id(), name);
    }
    LOG.info("Successfully deleted {} from env {}/{} capacity config by {}.",
        name, envName, stageName, operator);
  }

  public enum CapacityType {
    GROUP,
    HOST
  }
}
