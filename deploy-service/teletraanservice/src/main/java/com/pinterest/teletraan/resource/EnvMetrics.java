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
import com.pinterest.deployservice.bean.MetricsConfigBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo.Location;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@RolesAllowed({TeletraanPrincipalRole.Names.READ, TeletraanPrincipalRole.Names.READER})
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/metrics")
@Api(tags = "Environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(EnvWebHooks.class);
    private EnvironHandler environHandler;
    private ConfigHistoryHandler configHistoryHandler;
    private EnvironDAO environDAO;

    public EnvMetrics(@Context TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        environHandler = new EnvironHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
    }

    @GET
    @ApiOperation(
            value = "Get environment metrics",
            notes = "Returns a list of MetricsConfig object containing details for environment metrics gauges given " +
                    "an environment name and stage name",
            response = MetricsConfigBean.class, responseContainer = "List")
    public List<MetricsConfigBean> get(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName) throws Exception {
        EnvironBean environBean = Utils.getEnvStage(environDAO, envName, stageName);
        return environHandler.getMetrics(environBean);
    }

    @PUT
    @ApiOperation(
            value = "Update environment metrics",
            notes = "Updates an environment's metrics configs given an environment name, stage name, and list of " +
                    "MetricsConfig objects to update with")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = Location.PATH)
    public void update(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value = "List of MetricsConfigBean objects", required = true)@Valid List<MetricsConfigBean> metrics,
            @Context SecurityContext sc) throws Exception {
        EnvironBean environBean = Utils.getEnvStage(environDAO, envName, stageName);
        String userName = sc.getUserPrincipal().getName();
        environHandler.updateMetrics(environBean, metrics, userName);
        configHistoryHandler.updateConfigHistory(environBean.getEnv_id(), Constants.TYPE_ENV_METRIC, metrics, userName);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, environBean.getEnv_id(), Constants.TYPE_ENV_METRIC, userName, environBean.getExternal_id());
        LOG.info("Successfully updated metrics {} for env {}/{} by {}.",
            metrics, envName, stageName, userName);
    }
}
