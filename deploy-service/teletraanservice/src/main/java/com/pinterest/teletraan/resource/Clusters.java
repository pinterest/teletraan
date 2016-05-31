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

import com.pinterest.clusterservice.bean.ClusterInfoBean;
import com.pinterest.clusterservice.handler.ClusterHandler;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;

import com.google.common.base.Optional;

import io.swagger.annotations.Api;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

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
import javax.ws.rs.core.Response;
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

    public enum ActionType {
        REPLACE,
        PAUSE_REPLACE,
        RESUME_REPLACE,
        CANCEL_REPLACE
    }

    private enum HostActionType {
        TERMINATE,
        FORCE_TERMINATE
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
                              @Valid  ClusterInfoBean clusterInfoBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        clusterHandler.createCluster(envName, stageName, clusterInfoBean, operator);

        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, clusterInfoBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, operator);
        LOG.info(String.format("Successfully create cluster for %s/%s by %s", envName, stageName, operator));
    }

    @PUT
    public void updateCluster(@Context SecurityContext sc,
                              @PathParam("envName") String envName,
                              @PathParam("stageName") String stageName,
                              @Valid ClusterInfoBean clusterInfoBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        clusterHandler.updateCluster(envName, stageName, clusterInfoBean, operator);

        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, clusterInfoBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_CLUSTER, operator);
        LOG.info(String.format("Successfully update cluster for %s/%s by %s", envName, stageName, operator));
    }

    @GET
    public ClusterInfoBean getCluster(@PathParam("envName") String envName,
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
    @Path("/actions")
    public void replaceCluster(@Context SecurityContext sc,
                               @PathParam("envName") String envName,
                               @PathParam("stageName") String stageName,
                               @NotEmpty @QueryParam("actionType") ActionType actionType) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        switch (actionType) {
            case REPLACE:
                clusterHandler.replaceCluster(envName, stageName);
                break;
            case PAUSE_REPLACE:
                clusterHandler.pauseReplace(envName, stageName);
                break;
            case RESUME_REPLACE:
                clusterHandler.enableReplace(envName, stageName);
                break;
            case CANCEL_REPLACE:
                clusterHandler.cancelReplace(envName, stageName);
                break;
            default:
                throw new TeletaanInternalException(Response.Status.BAD_REQUEST, "No action found.");
        }

        String configChange = String.format("%s cluster replacement for %s/%s", actionType, envName, stageName);
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_OTHER, configChange, operator);
        LOG.info(String.format("Successfully %s cluster replacement for %s/%s by %s", actionType, envName, stageName, operator));
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
