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

import com.pinterest.clusterservice.bean.ClusterBean;
import com.pinterest.clusterservice.handler.ClusterHandler;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;

import com.google.common.base.Optional;

import io.swagger.annotations.Api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

import javax.validation.Valid;
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

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/clusters")
@Api(tags = "Environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Clusters {
    private static final Logger LOG = LoggerFactory.getLogger(Clusters.class);
    private final Authorizer authorizer;
    private final EnvironDAO environDAO;
    private final ClusterHandler clusterHandler;
    private final ConfigHistoryHandler configHistoryHandler;

    private enum HostActionType {
        TERMINATE,
        FORCE_TERMINATE
    }

    private enum ClusterActionType {
        REPLACE,
        MIGRATE
    }

    public Clusters(TeletraanServiceContext context) {
        authorizer = context.getAuthorizer();
        environDAO = context.getEnvironDAO();
        clusterHandler = new ClusterHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
    }

    @POST
    public void createCluster(@Context SecurityContext sc,
                              @PathParam("envName") String envName,
                              @PathParam("stageName") String stageName,
                              @Valid ClusterBean clusterBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        clusterHandler.createCluster(envName, stageName, clusterBean);

        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, clusterBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, operator);
        LOG.info(String.format("Successfully create cluster for %s/%s by %s", envName, stageName, operator));
    }

    @PUT
    public void updateCluster(@Context SecurityContext sc,
                              @PathParam("envName") String envName,
                              @PathParam("stageName") String stageName,
                              @Valid ClusterBean clusterBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        clusterHandler.updateCluster(envName, stageName, clusterBean);

        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, clusterBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, operator);
        LOG.info(String.format("Successfully update cluster for %s/%s by %s", envName, stageName, operator));
    }

    @GET
    public ClusterBean getCluster(@PathParam("envName") String envName,
                                  @PathParam("stageName") String stageName) throws Exception {
        return clusterHandler.getCluster(envName, stageName);
    }

    @DELETE
    public void deleteCluster(@Context SecurityContext sc,
                              @PathParam("envName") String envName,
                              @PathParam("stageName") String stageName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        clusterHandler.deleteCluster(envName, stageName);

        String configChange = String.format("delete cluster for %s/%s", envName, stageName);
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_OTHER, configChange, operator);
        LOG.info(String.format("Successfully delete cluster for %s/%s by %s", envName, stageName, operator));
    }

    @PUT
    @Path("/replacement")
    public void replaceCluster(@Context SecurityContext sc,
                               @PathParam("envName") String envName,
                               @PathParam("stageName") String stageName,
                               @QueryParam("type") String type,
                               @QueryParam("source") String source) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        ClusterActionType actionType = ClusterActionType.valueOf(ClusterActionType.class, type.toUpperCase());
        String configChange;
        if (actionType == ClusterActionType.MIGRATE) {
            clusterHandler.replaceCluster(envName, stageName, source);
            configChange = String.format("migrate cluster from %s for %s/%s", source, envName, stageName);
        } else {
            clusterHandler.replaceCluster(envName, stageName, null);
            configChange = String.format("replace cluster for %s/%s", envName, stageName);
        }
        LOG.info(String.format("Successfully %s by %s", configChange, operator));
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_OTHER, configChange, operator);
    }

    @GET
    @Path("/replacement")
    public boolean isScheduledForClusterReplacement(@PathParam("envName") String envName,
                                                    @PathParam("stageName") String stageName) throws Exception {
        return clusterHandler.isScheduledForClusterReplacement(envName, stageName);
    }

    @PUT
    @Path("/configs")
    public String updateAdvancedConfigs(@Context SecurityContext sc,
                                      @PathParam("envName") String envName,
                                      @PathParam("stageName") String stageName,
                                      @Valid Map<String, String> configs) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String configId = clusterHandler.updateAdvancedConfigs(envName, stageName, configs, operator);
        LOG.info(String.format("Successfully update cluster advanced setting for %s/%s by %s", envName, stageName, operator));
        return String.format("\"%s\"", configId);
    }

    @GET
    @Path("/configs")
    public Map<String, String> getAdvancedConfigs(@PathParam("envName") String envName,
                                                  @PathParam("stageName") String stageName) throws Exception {
        return clusterHandler.getAdvancedConfigs(envName, stageName);
    }

    @PUT
    @Path("/hosts")
    public void launchHosts(@Context SecurityContext sc,
                            @PathParam("envName") String envName,
                            @PathParam("stageName") String stageName,
                            @QueryParam("num") Optional<Integer> num) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        clusterHandler.launchHosts(envName, stageName, num.or(1));

        String configChange = String.format("launch %d hosts", num.or(1));
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_HOST_LAUNCH, configChange, operator);
        LOG.info(String.format("Successfully launch %d hosts for %s/%s by %s", num.or(1), envName, stageName, operator));
    }

    @DELETE
    @Path("/hosts")
    public void terminateHosts(@Context SecurityContext sc,
                               @PathParam("envName") String envName,
                               @PathParam("stageName") String stageName,
                               @Valid Collection<String> hostIds,
                               @QueryParam("type") String type) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        HostActionType actionType = HostActionType.valueOf(HostActionType.class, type.toUpperCase());
        if (actionType == HostActionType.FORCE_TERMINATE) {
            clusterHandler.terminateHosts(envName, stageName, hostIds);
        } else {
            clusterHandler.stopHosts(envName, stageName, hostIds);
        }

        String configChange = String.format("%s hostIds: %s", type, hostIds.toString());
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_HOST_TERMINATE, configChange, operator);
        LOG.info(String.format("Successfully schedule to %s hosts for %s/%s by %s\nhostIds: %s", type, envName, stageName, operator, hostIds.toString()));
    }

    @GET
    @Path("/hosts")
    public Collection<String> getHostNames(@PathParam("envName") String envName,
                                           @PathParam("stageName") String stageName) throws Exception {
        return clusterHandler.getHostNames(envName, stageName);
    }


}
