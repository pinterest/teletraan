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

import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.AgentErrorDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import io.swagger.annotations.*;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@PermitAll
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/agents")
@Api(tags = "Agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvAgents {
    private static final Logger LOG = LoggerFactory.getLogger(EnvAgents.class);
    private EnvironDAO environDAO;
    private AgentDAO agentDAO;
    private AgentErrorDAO agentErrorDAO;

    public enum CountActionType {
        SERVING,
        SERVING_AND_NORMAL,
        FIRST_DEPLOY,
        FAILED_FIRST_DEPLOY
    }

    public EnvAgents(@Context TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        agentDAO = context.getAgentDAO();
        agentErrorDAO = context.getAgentErrorDAO();
    }

    @GET
    @ApiOperation(
            value = "Get deploy agents",
            notes = "Returns a list of all the deploy agent objects for a given environment name and stage name",
            response = AgentBean.class, responseContainer = "List")
    public List<AgentBean> getAllAgents(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return agentDAO.getAllByEnv(envBean.getEnv_id());
    }

    @GET
    @Path("/errors/{hostName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get deploy agent error",
            notes = "Returns an AgentError object given an environment name, stage name, and host name",
            response = AgentErrorBean.class)
    public AgentErrorBean getAgentError(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value = "Host name", required = true)@PathParam("hostName") String hostName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        AgentErrorBean agentErrorBean = agentErrorDAO.get(hostName, envBean.getEnv_id());
        if (agentErrorBean == null) {
            return new AgentErrorBean();
        }
        return agentErrorBean;
    }

    @PUT
    @Path("/{hostId : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Update host agent",
            notes = "Updates host agent specified by given environment name, stage name, and host id with given " +
                    "agent object")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = ResourceAuthZInfo.Location.PATH)
    public void update(
            @Context SecurityContext sc,
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value = "Host id", required = true)@PathParam("hostId") String hostId,
            @ApiParam(value = "Agent object to update with", required = true)AgentBean agentBean) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        String operator = sc.getUserPrincipal().getName();
        agentDAO.update(hostId, envBean.getEnv_id(), agentBean);
        LOG.info("Successfully updated agent {} with {} in env {}/{} by {}.",
            hostId, agentBean, envName, stageName, operator);
    }

    @PUT
    @Path("/reset_failed_agents/{deployId : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Reset failed deploys",
            notes = "Resets failing deploys given an environment name, stage name, and deploy id")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = ResourceAuthZInfo.Location.PATH)
    public void resetFailedDeploys(
            @Context SecurityContext sc,
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value = "Deploy id", required = true)@PathParam("deployId") String deployId) throws Exception {
        EnvironBean environBean = Utils.getEnvStage(environDAO, envName, stageName);
        String operator = sc.getUserPrincipal().getName();
        agentDAO.resetFailedAgents(environBean.getEnv_id(), deployId);
        LOG.info("Successfully reset failed agents for deploy {} in env {}/{} by {}.",
            deployId, envName, stageName, operator);
    }

    @GET
    @Path("/count")
    public long countServingAgents(@PathParam("envName") String envName,
                                   @PathParam("stageName") String stageName,
                                   @NotNull @QueryParam("actionType") CountActionType actionType) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        if (envBean == null) {
            return 0;
        }

        long count = 0;
        switch (actionType) {
            case SERVING:
                count = agentDAO.countServingTotal(envBean.getEnv_id());
                break;
            case SERVING_AND_NORMAL:
                count = agentDAO.countServingAndNormalTotal(envBean.getEnv_id());
                break;
            case FIRST_DEPLOY:
                count = agentDAO.countFirstDeployingAgent(envBean.getEnv_id());
                break;
            case FAILED_FIRST_DEPLOY:
                count = agentDAO.countFailedFirstDeployingAgent(envBean.getEnv_id());
                break;
            default:
                throw new WebApplicationException("No action found.", Response.Status.BAD_REQUEST);
        }
        return count;
    }
}
