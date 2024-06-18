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

import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import io.swagger.annotations.*;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

@RolesAllowed({TeletraanPrincipalRole.Names.READ, TeletraanPrincipalRole.Names.READER})
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/user_roles")
@Api(tags = "User Roles")
@SwaggerDefinition(
        tags = {
                @Tag(name = "User Roles", description = "User Roles related APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvUserRoles extends UserRoles {
    private static final AuthZResource.Type RESOURCE_TYPE = AuthZResource.Type.ENV;

    public EnvUserRoles(@Context TeletraanServiceContext context) {
        super(context);
    }

    @GET
    @ApiOperation(
            value = "Get all environment user roles",
            notes = "Returns a list of UserRoles objects for the given environment name.",
            response = UserRolesBean.class, responseContainer = "List")
    public List<UserRolesBean> getByResource(
            @ApiParam(value = "Environment name.", required = true)@PathParam("envName") String envName) throws Exception {
        return super.getByResource(envName, RESOURCE_TYPE);
    }

    @GET
    @Path("/{userName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get user role by user and environment name",
            notes = "Returns a UserRoles object containing for given user and environment names.",
            response = UserRolesBean.class)
    public UserRolesBean getByNameAndResource(
            @ApiParam(value = "Environment name.", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "User name.", required = true)@PathParam("userName") String userName) throws Exception {
        return super.getByNameAndResource(userName, envName, RESOURCE_TYPE);
    }

    @PUT
    @Path("/{userName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Update a user's environment role",
            notes = "Updates a UserRoles object for given user and environment names with given UserRoles object.",
            response = UserRolesBean.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
    public void update(@ApiParam(value = "Environment name.", required = true) @PathParam("envName") String envName,
            @ApiParam(value = "User name.", required = true)@PathParam("userName") String userName, UserRolesBean bean) throws Exception {
        super.update(bean, userName, envName, RESOURCE_TYPE);
    }

    @POST
    @ApiOperation(
            value = "Create a user for an environment",
            notes = "Creates a new UserRoles object for a given environment name.",
            response = Response.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
    public Response create(@Context UriInfo uriInfo,
            @ApiParam(value = "Environment name.", required = true) @PathParam("envName") String envName,
            @ApiParam(value = "UserRolesBean object.", required = true)@Valid UserRolesBean bean) throws Exception {
        return super.create(uriInfo, bean, envName, RESOURCE_TYPE);
    }

    @DELETE
    @Path("/{userName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Deletes a user's roles from an environment",
            notes = "Deletes a UserRoles object by given user and environment names.")
    @RolesAllowed(TeletraanPrincipalRole.Names.DELETE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
    public void delete(@ApiParam(value = "Host name.", required = true) @PathParam("envName") String envName,
            @PathParam("userName") String userName) throws Exception {
        super.delete(userName, envName, RESOURCE_TYPE);
    }
}
