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

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/agent_configs")
@Api(value = "/Environments", description = "Environment info APIs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvAgentConfigs {
    private static final Logger LOG = LoggerFactory.getLogger(EnvAgentConfigs.class);
    private EnvironDAO environDAO;
    private EnvironHandler environHandler;
    private ConfigHistoryHandler configHistoryHandler;
    private Authorizer authorizer;

    @Context
    UriInfo uriInfo;

    public EnvAgentConfigs(TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        environHandler = new EnvironHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
        authorizer = context.getAuthorizer();
    }

    @GET
    @ApiOperation(
            value = "Get agent configs",
            notes = "Returns a name,value map of environment agent configs given an environment name and stage name",
            response = String.class, responseContainer = "Map")
    public Map<String, String> get(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return environHandler.getAdvancedConfigs(envBean);
    }

    @PUT
    @ApiOperation(
            value = "Update agent configs",
            notes = "Updates environment agent configs given an environment name and stage name with a map of " +
                    "name,value agent configs")
    public void update(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value = "Map of configs to update with", required = true)@Valid Map<String, String> configs,
            @Context SecurityContext sc) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String userName = sc.getUserPrincipal().getName();
        Utils.trimMapValues(configs);
        environHandler.updateAdvancedConfigs(envBean, configs, userName);
        configHistoryHandler.updateConfigHistory(envBean.getEnv_id(), Constants.TYPE_ENV_ADVANCED, configs, userName);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, envBean.getEnv_id(), Constants.TYPE_ENV_ADVANCED, userName);
        LOG.info("Successfully updated agent config {} for env {}/{} by {}.",
            configs, envName, stageName, userName);
    }
}
