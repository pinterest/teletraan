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
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Collection;
import java.util.List;
import io.swagger.annotations.*;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/groups")
@Api(tags = "Hosts and Systems")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Groups {

    public enum HostInfoActionType {
        ALL,
        RETIRED,
        FAILED,
        RETIRED_AND_FAILED,
        NEW_AND_SERVING_BUILD,
        NEW,
        TERMINATING
    }

    private EnvironDAO environDAO;
    private HostDAO hostDAO;

    public Groups(@Context TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        hostDAO = context.getHostDAO();
    }

    @GET
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/instances")
    public List<HostBean> getHostsByGroup(@PathParam("groupName") String groupName) throws Exception {
        return hostDAO.getAllActiveHostsByGroup(groupName);
    }

    @GET
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/hosts")
    public Collection<String> getHostIds(@PathParam("groupName") String groupName,
                                         @NotNull @QueryParam("actionType") HostInfoActionType actionType) throws Exception {
        Collection<String> hostIds;
        switch (actionType) {
            case ALL:
                hostIds = hostDAO.getHostIdsByGroup(groupName);
                break;
            case RETIRED:
                hostIds = hostDAO.getToBeRetiredHostIdsByGroup(groupName);
                break;
            case FAILED:
                hostIds = hostDAO.getFailedHostIdsByGroup(groupName);
                break;
            case RETIRED_AND_FAILED:
                hostIds = hostDAO.getToBeRetiredAndFailedHostIdsByGroup(groupName);
                break;
            case NEW_AND_SERVING_BUILD:
                hostIds = hostDAO.getNewAndServingBuildHostIdsByGroup(groupName);
                break;
            case NEW:
                hostIds = hostDAO.getNewHostIdsByGroup(groupName);
                break;
            case TERMINATING:
                hostIds = hostDAO.getTerminatingHostIdsByGroup(groupName);
                break;
            default:
                throw new WebApplicationException("No action found.", Response.Status.BAD_REQUEST);
        }
        return hostIds;
    }

    @GET
    @Path("/{groupName: [a-zA-Z0-9\\-_]+}/env")
    public EnvironBean getByClusterName(@PathParam("groupName") String groupName) throws Exception {
        return environDAO.getByCluster(groupName);
    }
}
