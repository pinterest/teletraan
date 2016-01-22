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
import com.pinterest.deployservice.bean.PromoteBean;
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

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/promotes")
@Api(tags = "Environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvPromotes {
    private static final Logger LOG = LoggerFactory.getLogger(EnvPromotes.class);
    private EnvironHandler environHandler;
    private ConfigHistoryHandler configHistoryHandler;
    private EnvironDAO environDAO;
    private Authorizer authorizer;

    @Context
    UriInfo uriInfo;
    public EnvPromotes(TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        environHandler = new EnvironHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
        authorizer = context.getAuthorizer();
    }

    @GET
    @ApiOperation(
            value = "Get promote info",
            notes = "Returns a promote info object given environment and stage names",
            response = PromoteBean.class)
    public PromoteBean get(@ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
                           @ApiParam(value = "Stage name", required = true)
                           @PathParam("stageName") String stageName) throws Exception {
        return environHandler.getEnvPromote(envName, stageName);
    }

    @PUT
    @ApiOperation(
            value = "Update promote info",
            notes = "Updates promote info given environment and stage names by given promote info object")
    public void update(@Context SecurityContext sc,
                       @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
                       @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
                       @ApiParam(value = "Promote object to update with", required = true)
                           @Valid PromoteBean promoteBean) throws Exception {
        EnvironBean environBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(environBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        environHandler.updateEnvPromote(environBean, promoteBean, operator);
        configHistoryHandler.updateConfigHistory(environBean.getEnv_id(), Constants.TYPE_ENV_PROMOTE, promoteBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, environBean.getEnv_id(), Constants.TYPE_ENV_PROMOTE, operator);
        LOG.info("Successfully updated promote with {} to env {}/{} by {}.",
            promoteBean, envName, stageName, operator);
    }
}
