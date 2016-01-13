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
import com.pinterest.teletraan.TeletraanServiceContext;
import io.swagger.annotations.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

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
    private AgentDAO agentDAO;

    public Agents(TeletraanServiceContext context) {
        agentDAO = context.getAgentDAO();
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
}
