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

import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.HostDAO;

import com.pinterest.teletraan.TeletraanServiceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Path("/v1/agents")
@Api(tags = "Agents")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Agents", description = "Deploy agent information APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Agents {
    private static final Logger LOG = LoggerFactory.getLogger(Agents.class);
    private AgentDAO agentDAO;
    private HostDAO hostDAO;

    public Agents(TeletraanServiceContext context) {
        agentDAO = context.getAgentDAO();
        hostDAO = context.getHostDAO();
    }

    @GET
    @ApiOperation(
            value = "Get Deploy Agent Host Info",
            notes = "Returns a list of all the deploy agent objects running on the specified host",
            response = AgentBean.class, responseContainer = "List")
    @Path("/{hostName : [a-zA-Z0-9\\-_]+}")
    public List<AgentBean> get(
            @ApiParam(value = "Host name", required = true)@PathParam("hostName") String hostName) throws Exception {
        return agentDAO.getByHost(hostName);
    }

    @GET
    @Path("/id/{hostId : [a-zA-Z0-9\\-_]+}")
    public Collection<AgentBean> getById(@PathParam("hostId") String hostId) throws Exception {
        return agentDAO.getByHostId(hostId);
    }

    @PUT
    @Path("/id/{hostId : [a-zA-Z0-9\\-_]+}")
    public void updateById(@Context SecurityContext sc,
                           @PathParam("hostId") String hostId,
                           @Valid AgentBean agentBean) throws Exception {
        String operator = sc.getUserPrincipal().getName();
        agentDAO.updateAgentById(hostId, agentBean);
        LOG.info("Successfully update agents {} by {}: {}", hostId, operator, agentBean.toString());
    }

    @GET
    @Path("/env/{envId : [a-zA-Z0-9\\-_]+}/total")
    public long getCountByEnvName(
        @ApiParam(value = "Env Id", required = true)@PathParam("envId") String envId) throws Exception {
        return agentDAO.countAgentByEnv(envId);
    }

    @GET
    @Path("/hostcount")
    public long getCountTotalHosts() throws Exception {
        return agentDAO.countDeployedHosts();
    }

    @GET
    @Path("/env/{envId : [a-zA-Z0-9\\-_]+}/accountIds")
    @ApiOperation(
            value = "Get account id for a specific environment object",
            notes = "Returns a mapping object of host id and account id given an environment id",
            response = String.class, responseContainer = "Map")
    public Map<String, String> getAccountIds(
            @ApiParam(value = "Environment id", required = true)@PathParam("envId") String envId) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        List<AgentBean> agents = agentDAO.getAllByEnv(envId);
        for (int i = 0; i < agents.size(); i++) 
        {
            String hostId = agents.get(i).getHost_id();
            String accountId = hostDAO.getAccountIdByHostId(hostId);
            result.put(hostId, accountId);
        }
        return result;
    }
}
