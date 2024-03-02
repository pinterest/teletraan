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

import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.annotations.*;
import java.util.Collection;
import java.util.List;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PermitAll
@Path("/v1/agents")
@Api(tags = "Agents")
@SwaggerDefinition(
        tags = {
            @Tag(name = "Agents", description = "Deploy agent information APIs"),
        })
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Agents {
    private static final Logger LOG = LoggerFactory.getLogger(Agents.class);
    private AgentDAO agentDAO;

    public Agents(@Context TeletraanServiceContext context) {
        agentDAO = context.getAgentDAO();
    }

    @GET
    @ApiOperation(
            value = "Get Deploy Agent Host Info",
            notes = "Returns a list of all the deploy agent objects running on the specified host",
            response = AgentBean.class,
            responseContainer = "List")
    @Path("/{hostName : [a-zA-Z0-9\\-_]+}")
    public List<AgentBean> get(
            @ApiParam(value = "Host name", required = true) @PathParam("hostName") String hostName)
            throws Exception {
        return agentDAO.getByHost(hostName);
    }

    @GET
    @Path("/id/{hostId : [a-zA-Z0-9\\-_]+}")
    public Collection<AgentBean> getById(@PathParam("hostId") String hostId) throws Exception {
        return agentDAO.getByHostId(hostId);
    }

    @PUT
    @Path("/id/{hostId : [a-zA-Z0-9\\-_]+}")
    @RolesAllowed(TeletraanPrincipalRole.Names.EXECUTE)
    @ResourceAuthZInfo(type = AuthZResource.Type.HOST, idLocation = ResourceAuthZInfo.Location.PATH)
    public void updateById(
            @Context SecurityContext sc,
            @PathParam("hostId") String hostId,
            @Valid AgentBean agentBean)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();
        agentDAO.updateAgentById(hostId, agentBean);
        LOG.info("Successfully update agents {} by {}: {}", hostId, operator, agentBean);
    }

    @GET
    @Path("/env/{envId : [a-zA-Z0-9\\-_]+}/total")
    public long getCountByEnvName(
            @ApiParam(value = "Env Id", required = true) @PathParam("envId") String envId)
            throws Exception {
        return agentDAO.countAgentByEnv(envId);
    }

    @GET
    @Path("/hostcount")
    public long getCountTotalHosts() throws Exception {
        return agentDAO.countDeployedHosts();
    }
}
