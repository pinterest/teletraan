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
import java.util.List;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/group_roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvGroupRoles extends GroupRoles {
    private static final Resource.Type RESOURCE_TYPE = Resource.Type.ENV;

    public EnvGroupRoles(TeletraanServiceContext context) {
        super(context);
    }

    @GET
    public List<GroupRolesBean> getByResource(
        @PathParam("envName") String envName) throws Exception {
        return super.getByResource(envName, RESOURCE_TYPE);
    }

    @GET
    @Path("/{groupName : [a-zA-Z0-9\\-_]+}")
    public GroupRolesBean getByNameAndResource(@PathParam("envName") String envName,
        @PathParam("groupName") String groupName) throws Exception {
        return super.getByNameAndResource(groupName, envName, RESOURCE_TYPE);
    }

    @PUT
    @Path("/{groupName : [a-zA-Z0-9\\-_]+}")
    public void update(@Context SecurityContext sc, @PathParam("envName") String envName,
        @PathParam("groupName") String groupName,
        GroupRolesBean bean) throws Exception {
        super.update(sc, bean, groupName, envName, RESOURCE_TYPE);
    }

    @POST
    public void create(@Context SecurityContext sc, @PathParam("envName") String envName,
        @Valid GroupRolesBean bean) throws Exception {
        super.create(sc, bean, envName, RESOURCE_TYPE);
    }

    @DELETE
    @Path("/{groupName : [a-zA-Z0-9\\-_]+}")
    public void delete(@Context SecurityContext sc, @PathParam("envName") String envName,
        @PathParam("groupName") String groupName) throws Exception {
        super.delete(sc, groupName, envName, RESOURCE_TYPE);
    }
}
