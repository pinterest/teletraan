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

import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.handler.DeployHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;
import com.pinterest.clusterservice.cm.ClusterManager;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/deploys")
@Api(tags = "Deploys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvDeploys {
    public enum ActionType {
        PROMOTE, RESTART, ROLLBACK, PAUSE, RESUME
    }

    public enum HostActions {
        PAUSE, RESET
    }

    private static final Logger LOG = LoggerFactory.getLogger(EnvDeploys.class);
    private EnvironDAO environDAO;
    private DeployDAO deployDAO;
    private Authorizer authorizer;
    private EnvironHandler environHandler;
    private DeployHandler deployHandler;
    private AgentDAO agentDAO;
    private ClusterManager clusterManager;

    @Context
    UriInfo uriInfo;

    public EnvDeploys(TeletraanServiceContext context) throws Exception {
        environDAO = context.getEnvironDAO();
        deployDAO = context.getDeployDAO();
        authorizer = context.getAuthorizer();
        environHandler = new EnvironHandler(context);
        deployHandler = new DeployHandler(context);
        agentDAO = context.getAgentDAO();
        clusterManager = context.getClusterManager();
    }

    @GET
    @Path("/current")
    @ApiOperation(
            value = "Get deploy info by environment",
            notes = "Returns a deploy info object given an environment name and stage name",
            response = DeployBean.class)
    public DeployBean get(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return deployHandler.getDeploySafely(envBean.getDeploy_id());
    }

    @POST
    @Path("/current/actions")
    @ApiOperation(
            value = "Take deploy action",
            notes = "Take an action on a deploy such as RESTART or PAUSE",
            response = Response.class)
    public Response action(
            @Context SecurityContext sc,
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value = "ActionType enum selection", required = true)@NotEmpty @QueryParam("actionType") ActionType actionType,
            @ApiParam(value = "Lower bound deploy id", required = true)@QueryParam("fromDeployId") String fromDeployId,
            @ApiParam(value = "Upper bound deploy id", required = true)@QueryParam("toDeployId") String toDeployId,
            @ApiParam(value = "Description", required = true)@QueryParam("description") String description) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String newDeployId;
        switch (actionType) {
            case RESTART:
                newDeployId = deployHandler.restart(envBean, description, operator);
                break;
            case PAUSE:
                newDeployId = environHandler.pause(envBean, operator);
                break;
            case RESUME:
                newDeployId = environHandler.resume(envBean, operator);
                break;
            case ROLLBACK:
                newDeployId = deployHandler.rollback(envBean, toDeployId, description, operator);
                break;
            case PROMOTE:
                newDeployId = deployHandler.promote(envBean, fromDeployId, description, operator);
                break;
            default:
                throw new TeletaanInternalException(Response.Status.BAD_REQUEST, "No action found.");
        }
        LOG.info("Successfully create deploy {} for env{}/{} as {} by {}.",
            newDeployId, envName, stageName, actionType, operator);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI deployUri = ub.path("current").build();
        DeployBean result = deployDAO.getById(newDeployId);
        return Response.created(deployUri).entity(result).build();
    }

    @POST
    @Path("/hostactions")
    @ApiOperation(
        value = "Take a deploy action",
        notes = "Take an action on a deploy using host information",
        response = Response.class)
    public Response action(
            @Context SecurityContext sc,
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName, 
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value = "HostActions enum selection", required = true)@QueryParam("actionType") HostActions actionType,
            @ApiParam(value = "List of ids", required = true)@QueryParam("hostIds") Collection<String> hostIds) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        AgentBean agentBean = new AgentBean();
        switch (actionType) {
            case PAUSE:
                agentBean.setState(AgentState.PAUSED_BY_USER);
                agentBean.setLast_update(System.currentTimeMillis());
                agentDAO.updateMultiple(hostIds, envBean.getEnv_id(), agentBean);
            case RESET: 
                agentBean.setState(AgentState.RESET);
                agentBean.setLast_update(System.currentTimeMillis());
                agentDAO.updateMultiple(hostIds, envBean.getEnv_id(), agentBean);
            default:
                throw new TeletaanInternalException(Response.Status.BAD_REQUEST, "No action found.");
        }
    }
        
    @ApiOperation(
            value = "Create a deploy",
            notes = "Creates a deploy given an environment name, stage name, build id and description",
            response = Response.class)
    public Response create(
            @Context SecurityContext sc,
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value = "Build id", required = true)@NotEmpty @QueryParam("buildId") String buildId,
            @ApiParam(value = "Description", required = true)@QueryParam("description") String description) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        String deployId = deployHandler.deploy(envBean, buildId, description, operator);
        LOG.info("Successfully create deploy {} for env {}/{} by {}.", deployId, envName, stageName, operator);

        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI deployUri = ub.path("current").build();
        DeployBean deployBean = deployDAO.getById(deployId);
        return Response.created(deployUri).entity(deployBean).build();
    }

    // Even though this is PUT, but it really just update progress, no check is needed
    @PUT
    @Path("/current/progress")
    @ApiOperation(
            value = "Update deploy progress",
            notes = "Updates a deploy's progress given an environment name and stage name and returns a deploy " +
                    "progress object",
            response = DeployProgressBean.class)
    public DeployProgressBean updateProgress(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        if (envBean.getDeploy_id() == null) {
            DeployProgressBean emptyProgress = new DeployProgressBean();
            emptyProgress.setAgents(Collections.emptyList());
            emptyProgress.setMissingHosts(Collections.emptyList());
            emptyProgress.setProvisioningHosts(Collections.emptyList());
            return emptyProgress;
        }
        return environHandler.updateDeployProgress(envBean);
    }

    @GET
    @Path("/current/missing-hosts")
    @ApiOperation(
            value = "Get missing hosts for stage",
            notes = "Returns a list of missing hosts given an environment and stage",
            response = String.class, responseContainer = "List")
    public List<String> getMissingHosts(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return new ArrayList<>(environDAO.getMissingHosts(envBean.getEnv_id()));
    }
}
