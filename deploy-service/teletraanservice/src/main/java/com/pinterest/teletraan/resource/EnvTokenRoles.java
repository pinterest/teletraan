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

import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/token_roles")
@Api(value = "Script Tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvTokenRoles extends TokenRoles {
    private static final AuthZResource.Type RESOURCE_TYPE = AuthZResource.Type.ENV;

    public EnvTokenRoles(@Context TeletraanServiceContext context) {
        super(context);
    }

    @GET
    @ApiOperation(
            value = "Get environment TokenRoles objects",
            notes = "Returns all the TokenRoles objects for a given environment.",
            response = TokenRolesBean.class,
            responseContainer = "List")
    @RolesAllowed(TeletraanPrincipalRole.Names.READ)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
    public List<TokenRolesBean> getByResource(
            @ApiParam(value = "Environment name.", required = true) @PathParam("envName")
                    String envName)
            throws Exception {
        return super.getByResource(envName, RESOURCE_TYPE);
    }

    @GET
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get TokenRoles object by script and environment names",
            notes = "Returns a TokenRoles object given a script and environment name.",
            response = TokenRolesBean.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.READ)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
    public TokenRolesBean getByNameAndResource(
            @ApiParam(value = "Environment name.", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "Script name.", required = true) @PathParam("scriptName")
                    String scriptName)
            throws Exception {
        return super.getByNameAndResource(scriptName, envName, RESOURCE_TYPE);
    }

    @PUT
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Update an envrionment's script token",
            notes =
                    "Update a specific environment script token given environment and script names.")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
    public void update(
            @ApiParam(value = "Environment name.", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "Script name.", required = true) @PathParam("scriptName")
                    String scriptName,
            TokenRolesBean bean)
            throws Exception {
        super.update(bean, scriptName, envName, RESOURCE_TYPE);
    }

    @POST
    @ApiOperation(
            value = "Create an environment script token",
            notes =
                    "Creates an environment script token with given environment name and TokenRoles object.",
            response = Response.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
    public Response create(
            @Context UriInfo uriInfo,
            @ApiParam(value = "Environment name.", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "TokenRolesBean object.", required = true) @Valid TokenRolesBean bean)
            throws Exception {
        return super.create(uriInfo, bean, envName, RESOURCE_TYPE);
    }

    @DELETE
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Delete an environment script token",
            notes = "Deletes a script token by given environment and script name.")
    @RolesAllowed(TeletraanPrincipalRole.Names.DELETE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
    public void delete(
            @ApiParam(value = "Environment name.", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "Script name.", required = true) @PathParam("scriptName")
                    String scriptName)
            throws Exception {
        super.delete(scriptName, envName, RESOURCE_TYPE);
    }
}
