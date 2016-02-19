/*
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pinterest.teletraan.resource;


import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.teletraan.TeletraanServiceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/hosts")
@Api(tags = "Hosts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvHosts {
    private static final Logger LOG = LoggerFactory.getLogger(EnvHosts.class);
    private final EnvironDAO environDAO;
    private final HostDAO hostDAO;

    public EnvHosts(TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        hostDAO = context.getHostDAO();
    }

    @GET
    @ApiOperation(
            value = "Get hosts for env stage",
            notes = "Returns a Collections of hosts given an environment and stage",
            response = HostBean.class, responseContainer = "Collection")
    public Collection<HostBean> get(
            @ApiParam(value = "Environment name", required = true) @PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true) @PathParam("stageName") String stageName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return hostDAO.getHostsByEnvId(envBean.getEnv_id());
    }

    @GET
    @Path("/{hostName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get host details for stage and host name",
            notes = "Returns a host given an environment, stage and host name",
            response = HostBean.class)
    public HostBean getHostByHostName(
            @ApiParam(value = "Environment name", required = true) @PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true) @PathParam("stageName") String stageName,
            @ApiParam(value = "Host name", required = true) @PathParam("hostName") String hostName) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return hostDAO.getByEnvIdAndHostName(envBean.getEnv_id(), hostName);
    }
}
