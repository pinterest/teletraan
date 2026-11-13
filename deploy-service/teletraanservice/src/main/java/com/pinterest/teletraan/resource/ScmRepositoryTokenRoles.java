/**
 * Copyright (c) 2026 Pinterest, Inc.
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
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/** Manages read-only script tokens for an explicitly authorized source repository. */
@RolesAllowed(TeletraanPrincipalRole.Names.MANAGE_SCRIPT_TOKEN)
@Path("/v1/scm_repositories/token_roles")
@Api(tags = "Script Tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScmRepositoryTokenRoles extends TokenRoles {
    private static final AuthZResource.Type RESOURCE_TYPE = AuthZResource.Type.SCM_REPOSITORY;
    private final TeletraanServiceContext context;

    public ScmRepositoryTokenRoles(@Context TeletraanServiceContext context) {
        super(context);
        this.context = context;
    }

    @GET
    @ApiOperation(
            value = "List repository script tokens",
            response = TokenRolesBean.class,
            responseContainer = "List")
    @RolesAllowed(TeletraanPrincipalRole.Names.MANAGE_SCRIPT_TOKEN)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.SCM_REPOSITORY,
            idLocation = ResourceAuthZInfo.Location.QUERY)
    public List<TokenRolesBean> getByResource(
            @QueryParam("scm") String scm, @QueryParam("repo") String repo) throws Exception {
        return super.getByResource(repositoryResourceName(scm, repo), RESOURCE_TYPE);
    }

    @GET
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(value = "Get repository script token metadata", response = TokenRolesBean.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.MANAGE_SCRIPT_TOKEN)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.SCM_REPOSITORY,
            idLocation = ResourceAuthZInfo.Location.QUERY)
    public TokenRolesBean getByNameAndResource(
            @PathParam("scriptName") String scriptName,
            @QueryParam("scm") String scm,
            @QueryParam("repo") String repo)
            throws Exception {
        return super.getByNameAndResource(
                scriptName, repositoryResourceName(scm, repo), RESOURCE_TYPE);
    }

    @POST
    @ApiOperation(
            value = "Create a read-only repository script token",
            notes =
                    "Only the script name is accepted from the body. The server assigns a READ role,"
                            + " exact repository scope, random bearer secret and finite expiry.")
    @RolesAllowed(TeletraanPrincipalRole.Names.MANAGE_SCRIPT_TOKEN)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.SCM_REPOSITORY,
            idLocation = ResourceAuthZInfo.Location.QUERY)
    public Response create(
            @Context UriInfo uriInfo,
            @QueryParam("scm") String scm,
            @QueryParam("repo") String repo,
            TokenRolesBean requestedToken)
            throws Exception {
        if (requestedToken == null
                || requestedToken.getScript_name() == null
                || !requestedToken.getScript_name().matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new BadRequestException(
                    "A script name of 1 to 64 letters, digits, underscores or hyphens is required");
        }
        TokenRolesBean token = new TokenRolesBean();
        token.setScript_name(requestedToken.getScript_name());
        token.setRole(TeletraanPrincipalRole.READ);
        Response created =
                super.create(uriInfo, token, repositoryResourceName(scm, repo), RESOURCE_TYPE);

        // The repository query is part of this resource's identity and must survive Location links.
        // Jersey preserves existing escapes in queryParam values; these inputs are decoded text.
        UriBuilder location =
                UriBuilder.fromUri(created.getLocation())
                        .queryParam("repo", repo.replace("%", "%25"));
        if (scm != null) {
            location.queryParam("scm", scm.replace("%", "%25"));
        }
        return Response.fromResponse(created).location(location.build()).build();
    }

    @DELETE
    @Path("/{scriptName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(value = "Revoke a repository script token")
    @RolesAllowed(TeletraanPrincipalRole.Names.MANAGE_SCRIPT_TOKEN)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.SCM_REPOSITORY,
            idLocation = ResourceAuthZInfo.Location.QUERY)
    public void delete(
            @PathParam("scriptName") String scriptName,
            @QueryParam("scm") String scm,
            @QueryParam("repo") String repo)
            throws Exception {
        super.delete(scriptName, repositoryResourceName(scm, repo), RESOURCE_TYPE);
    }

    private String repositoryResourceName(String scm, String repo) {
        return context.getSourceControlManagerProxy().getRepositoryResourceName(scm, repo);
    }
}
