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
import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

@Path("/v1/system/token_roles")
@Api(tags = "Script Tokens")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Script Tokens", description = "Internal script tokens APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SystemTokenRoles extends TokenRoles {
    private static final Resource.Type RESOURCE_TYPE = Resource.Type.SYSTEM;
    private final static String RESOURCE_ID = Resource.ALL;

    public SystemTokenRoles(TeletraanServiceContext context) {
        super(context);
    }

    @GET
    @ApiOperation(
            value = "Get system script tokens",
            notes = "Returns all system TokenRoles objects",
            response = TokenRolesBean.class, responseContainer = "List")
    public List<TokenRolesBean> getByResource(@Context SecurityContext sc) throws Exception {
        return super.getByResource(sc, RESOURCE_ID, RESOURCE_TYPE);
    }

    @GET
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get system TokenRoles object by script name",
            notes = "Returns a TokenRoles object for given script name",
            response = TokenRolesBean.class)
    public TokenRolesBean getByNameAndResource(@Context SecurityContext sc,
            @ApiParam(value = "Script name.", required = true)@PathParam("scriptName") String scriptName) throws Exception {
        return super.getByNameAndResource(sc, scriptName, RESOURCE_ID, RESOURCE_TYPE);
    }

    @PUT
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Update a system script token",
            notes = "Updates a TokenRoles object by given script name and replacement TokenRoles object")
    public void update(@Context SecurityContext sc,
                       @ApiParam(value = "Script name.", required = true)@PathParam("scriptName") String scriptName,
                       @ApiParam(value = "TokenRolesBean object.", required = true)TokenRolesBean bean) throws Exception {
        super.update(sc, bean, scriptName, RESOURCE_ID, RESOURCE_TYPE);
    }

    @POST
    @ApiOperation(
            value = "Create a system script token",
            notes = "Creates a specified system wide TokenRole and returns a Response object",
            response = Response.class)
    public Response create(@Context SecurityContext sc,
                           @Context UriInfo uriInfo,
                           @ApiParam(value = "TokenRolesBean object.", required = true)@Valid TokenRolesBean bean) throws Exception {
        return super.create(sc, uriInfo, bean, RESOURCE_ID, RESOURCE_TYPE);
    }

    @DELETE
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Delete a system wide script token",
            notes = "Deletes a system wide TokenRoles object by specified script name")
    public void delete(@Context SecurityContext sc,
                       @ApiParam(value = "Script name.", required = true)@PathParam("scriptName") String scriptName) throws Exception {
        super.delete(sc, scriptName, RESOURCE_ID, RESOURCE_TYPE);
    }
}
