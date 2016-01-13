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
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}")
@Api(tags = "Environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvStages {
    private static final Logger LOG = LoggerFactory.getLogger(EnvStages.class);
    private EnvironDAO environDAO;
    private EnvironHandler environHandler;
    private Authorizer authorizer;

    public EnvStages(TeletraanServiceContext context) throws Exception {
        environDAO = context.getEnvironDAO();
        environHandler = new EnvironHandler(context);
        authorizer = context.getAuthorizer();
    }

    @GET
    @ApiOperation(
            value = "Get an environment",
            notes = "Returns an environment object given environment and stage names",
            response = EnvironBean.class)
    public EnvironBean get(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName) throws Exception {
        return Utils.getEnvStage(environDAO, envName, stageName);
    }

    @PUT
    @ApiOperation(
            value = "Update an environment",
            notes = "Update an environment given environment and stage names with a environment object")
    public void update(@Context SecurityContext sc,
                       @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
                       @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
                       @ApiParam(value = "Desired Environment object with updates", required = true)
                           EnvironBean environBean) throws Exception {
        EnvironBean origBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(origBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        environHandler.updateStage(origBean, environBean, operator);
        LOG.info("Successfully updated env {}/{} with {} by {}.",
            envName, stageName, environBean, operator);
    }

    @DELETE
    @ApiOperation(
            value = "Delete an environment",
            notes = "Deletes an environment given a environment and stage names")
    public void delete(@Context SecurityContext sc,
                       @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
                       @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName)
                        throws Exception {
        EnvironBean origBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(origBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        environHandler.deleteEnvStage(envName, stageName, operator);
        LOG.info("Successfully deleted env {}/{} by {}.", envName, stageName, operator);
    }
}
