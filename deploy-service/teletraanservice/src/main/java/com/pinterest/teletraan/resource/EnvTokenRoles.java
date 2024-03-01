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

import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.teletraan.TeletraanServiceContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/token_roles")
@Api(value = "Script Tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvTokenRoles extends TokenRoles {
    private static final Resource.Type RESOURCE_TYPE = Resource.Type.ENV;

    public EnvTokenRoles(@Context TeletraanServiceContext context) {
        super(context);
    }

    @GET
    @ApiOperation(
            value = "Get environment TokenRoles objects",
            notes = "Returns all the TokenRoles objects for a given environment.",
            response = TokenRolesBean.class, responseContainer = "List")
    public List<TokenRolesBean> getByResource(@Context SecurityContext sc,
            @ApiParam(value = "Environment name.", required = true)@PathParam("envName") String envName) throws Exception {
        return super.getByResource(sc, envName, RESOURCE_TYPE);
    }

    @GET
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get TokenRoles object by script and environment names",
            notes = "Returns a TokenRoles object given a script and environment name.",
            response = TokenRolesBean.class)
    public TokenRolesBean getByNameAndResource(@Context SecurityContext sc,
            @ApiParam(value = "Environment name.", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Script name.", required = true)@PathParam("scriptName") String scriptName) throws Exception {
        return super.getByNameAndResource(sc, scriptName, envName, RESOURCE_TYPE);
    }

    @PUT
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Update an envrionment's script token",
            notes = "Update a specific environment script token given environment and script names.")
    public void update(@Context SecurityContext sc,
                       @ApiParam(value = "Environment name.", required = true)@PathParam("envName") String envName,
                       @ApiParam(value = "Script name.", required = true)@PathParam("scriptName") String scriptName, TokenRolesBean bean) throws Exception {
        super.update(sc, bean, scriptName, envName, RESOURCE_TYPE);
    }

    @POST
    @ApiOperation(
            value = "Create an environment script token",
            notes = "Creates an environment script token with given environment name and TokenRoles object.",
            response = Response.class)
    public Response create(@Context SecurityContext sc,
                           @Context UriInfo uriInfo,
                           @ApiParam(value = "Environment name.", required = true)@PathParam("envName") String envName,
                           @ApiParam(value = "TokenRolesBean object.", required = true)@Valid TokenRolesBean bean) throws Exception {
        return super.create(sc, uriInfo, bean, envName, RESOURCE_TYPE);
    }

    @DELETE
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Delete an environment script token",
            notes = "Deletes a script token by given environment and script name.")
    public void delete(@Context SecurityContext sc,
                       @ApiParam(value = "Environment name.", required = true)@PathParam("envName") String envName,
                       @ApiParam(value = "Script name.", required = true)@PathParam("scriptName") String scriptName) throws Exception {
        super.delete(sc, scriptName, envName, RESOURCE_TYPE);
    }
}
