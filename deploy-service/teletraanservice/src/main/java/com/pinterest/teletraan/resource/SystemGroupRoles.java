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

import com.pinterest.deployservice.bean.GroupRolesBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.teletraan.TeletraanServiceContext;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/v1/system/group_roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SystemGroupRoles extends GroupRoles {
    private final static Resource.Type RESOURCE_TYPE = Resource.Type.SYSTEM;
    private final static String RESOURCE_ID = Resource.ALL;

    public SystemGroupRoles(@Context TeletraanServiceContext context) {
        super(context);
    }

    @GET
    public List<GroupRolesBean> getByResource() throws Exception {
        return super.getByResource(RESOURCE_ID, RESOURCE_TYPE);
    }

    @GET
    @Path("/{groupName : [a-zA-Z0-9\\-_]+}")
    public GroupRolesBean getByNameAndResource(@PathParam("groupName") String groupName) throws Exception {
        return super.getByNameAndResource(groupName, RESOURCE_ID, RESOURCE_TYPE);
    }

    @PUT
    @Path("/{groupName : [a-zA-Z0-9\\-_]+}")
    public void update(@Context SecurityContext sc, @PathParam("groupName") String groupName,
        GroupRolesBean bean) throws Exception {
        super.update(sc, bean, groupName, RESOURCE_ID, RESOURCE_TYPE);
    }

    @POST
    public void create(@Context SecurityContext sc, @Context UriInfo uriInfo, @Valid GroupRolesBean bean) throws Exception {
        super.create(sc, uriInfo, bean, RESOURCE_ID, RESOURCE_TYPE);
    }

    @DELETE
    @Path("/{groupName : [a-zA-Z0-9\\-_]+}")
    public void delete(@Context SecurityContext sc, @PathParam("groupName") String groupName) throws Exception {
        super.delete(sc, groupName, RESOURCE_ID, RESOURCE_TYPE);
    }
}
