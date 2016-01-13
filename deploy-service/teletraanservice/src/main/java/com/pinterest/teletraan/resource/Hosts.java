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

import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import io.swagger.annotations.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/hosts")
@Api(tags = "Hosts and Systems")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Hosts and Systems", description = "Host info APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Hosts {
    private HostDAO hostDAO;

    public Hosts(TeletraanServiceContext context) {
        hostDAO = context.getHostDAO();
    }

    @GET
    @Path("/{hostName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get host info objects by host name",
            notes = "Returns a list of host info objects given a host name",
            response = HostBean.class, responseContainer = "List")
    public List<HostBean> get(
            @ApiParam(value = "Host name", required = true)@PathParam("hostName") String hostName) throws Exception {
        return hostDAO.getHosts(hostName);
    }
}
