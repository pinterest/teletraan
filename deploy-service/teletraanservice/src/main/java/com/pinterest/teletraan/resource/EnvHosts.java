/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostBeanWithStatuses;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/hosts")
@Api(tags = "Hosts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvHosts {
    private static final Logger LOG = LoggerFactory.getLogger(EnvHosts.class);
    private final EnvironDAO environDAO;
    private final HostDAO hostDAO;
    private final EnvironHandler environHandler;
    private final ConfigHistoryHandler configHistoryHandler;

    public EnvHosts(@Context TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        hostDAO = context.getHostDAO();
        environHandler = new EnvironHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
    }

    @GET
    @ApiOperation(
            value = "Get hosts for env stage",
            notes = "Returns a Collections of hosts given an environment and stage",
            response = HostBean.class,
            responseContainer = "List")
    public Collection<HostBean> get(
            @ApiParam(value = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "Stage name", required = true) @PathParam("stageName")
                    String stageName)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return hostDAO.getHostsByEnvId(envBean.getEnv_id());
    }

    @GET
    @Path("/{hostName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get host details for stage and host name",
            notes = "Returns a host given an environment, stage and host name",
            response = HostBean.class,
            responseContainer = "List")
    public Collection<HostBeanWithStatuses> getHostByHostName(
            @ApiParam(value = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "Stage name", required = true) @PathParam("stageName")
                    String stageName,
            @ApiParam(value = "Host name", required = true) @PathParam("hostName") String hostName)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return hostDAO.getByEnvIdAndHostName(envBean.getEnv_id(), hostName);
    }

    @DELETE
    @RolesAllowed(TeletraanPrincipalRole.Names.EXECUTE)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.ENV_STAGE,
            idLocation = ResourceAuthZInfo.Location.PATH)
    public void stopServiceOnHost(
            @Context SecurityContext sc,
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @Valid Collection<String> hostIds,
            @ApiParam(value = "Replace the host or not") @QueryParam("replaceHost")
                    Optional<Boolean> replaceHost)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        environHandler.ensureHostsOwnedByEnv(envBean, hostIds);
        environHandler.stopServiceOnHosts(hostIds, replaceHost.orElse(true));
        configHistoryHandler.updateConfigHistory(
                envBean.getEnv_id(),
                Constants.TYPE_HOST_ACTION,
                String.format("STOP %s", hostIds.toString()),
                operator);
        LOG.info(
                "Successfully stopped {}/{} service on hosts {} by {}",
                envName,
                stageName,
                hostIds,
                operator);
    }
}
