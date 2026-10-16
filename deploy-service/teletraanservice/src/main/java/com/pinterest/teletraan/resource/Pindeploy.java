/**
 * Copyright (c) 2024 Pinterest, Inc.
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
import com.pinterest.deployservice.bean.PindeployBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.PindeployDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.config.AuthorizationFactory;
import com.pinterest.teletraan.universal.security.TeletraanAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import io.swagger.annotations.*;
import java.security.Principal;
import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/pindeploy")
@Api(tags = "Pindeploy")
@SwaggerDefinition(
        tags = {
            @Tag(name = "Pindeploy", description = "Pindeploy related APIs"),
        })
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Pindeploy {
    private static final Logger LOG = LoggerFactory.getLogger(Pindeploy.class);
    private PindeployDAO pindeployDAO;
    private EnvironDAO environDAO;
    private AuthorizationFactory authorizationFactory;
    private TeletraanServiceContext context;

    public Pindeploy(@Context TeletraanServiceContext context) throws Exception {
        this.context = context;
        pindeployDAO = context.getPindeployDAO();
        environDAO = context.getEnvironDAO();
        authorizationFactory = context.getAuthorizationFactory();
    }

    /**
     * enable/disablePindeployPipeline carry no {@code @ResourceAuthZInfo} because their
     * envName/stageName/pipeline identifiers arrive as query params, a location the resource
     * extractor factory does not support (see TeletraanAuthZResourceExtractorFactory) -- adding
     * the annotation would 500 at request time rather than authorize. This performs the same
     * per-instance WRITE check the framework would have run, using the on-the-fly authorizer
     * pattern already used by EnvironmentHandler.validateSystemPriorityPermission.
     */
    private void requireWriteOnEnvStage(SecurityContext sc, String envName, String stageName) {
        Principal principal = sc.getUserPrincipal();
        if (!(principal instanceof TeletraanPrincipal)) {
            throw new ForbiddenException("Modifying pindeploy pipeline wiring requires an authenticated Teletraan principal");
        }
        TeletraanPrincipal teletraanPrincipal = (TeletraanPrincipal) principal;
        TeletraanAuthorizer<TeletraanPrincipal> authorizer =
                authorizationFactory.createSecondaryAuthorizer(context, teletraanPrincipal.getClass());
        AuthZResource envResource = new AuthZResource(envName, stageName);
        boolean isAuthorized =
                authorizer.authorize(
                        teletraanPrincipal, TeletraanPrincipalRole.Names.WRITE, envResource, null);
        if (!isAuthorized) {
            throw new ForbiddenException(
                    String.format(
                            "Modifying pindeploy pipeline wiring requires WRITE role on environment %s/%s",
                            envName, stageName));
        }
    }

    @GET
    @ApiOperation(
            value = "Get pindeploy related info",
            notes = "Return is_pindeploy and pipeline given the environment id",
            response = PindeployBean.class)
    public PindeployBean getPindeployInfo(
            @NotEmpty @QueryParam("envName") String envName,
            @NotEmpty @QueryParam("stageName") String stageName)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return pindeployDAO.get(envBean.getEnv_id());
    }

    @DELETE
    @Path("/disable")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    public void disablePindeployPipeline(
            @Context SecurityContext sc, @NotEmpty @QueryParam("pipeline") String pipeline)
            throws Exception {
        PindeployBean existing = pindeployDAO.getByPipeline(pipeline);
        if (existing != null) {
            EnvironBean owningEnv = environDAO.getById(existing.getEnv_id());
            if (owningEnv != null) {
                requireWriteOnEnvStage(sc, owningEnv.getEnv_name(), owningEnv.getStage_name());
            }
        }
        String operator = sc.getUserPrincipal().getName();
        pindeployDAO.delete(pipeline);
        LOG.info(
                String.format(
                        "Successfully disabled pindeploy pipeline %s by %s", pipeline, operator));
    }

    @POST
    @Path("/enable")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    public void enablePindeployPipeline(
            @Context SecurityContext sc,
            @NotEmpty @QueryParam("envName") String envName,
            @NotEmpty @QueryParam("stageName") String stageName,
            @NotEmpty @QueryParam("pipeline") String pipeline)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        requireWriteOnEnvStage(sc, envName, stageName);
        String operator = sc.getUserPrincipal().getName();
        PindeployBean pindeployBean = new PindeployBean();
        pindeployBean.setEnv_id(envBean.getEnv_id());
        pindeployBean.setPipeline(pipeline);
        pindeployBean.setIs_pindeploy(true);
        pindeployDAO.insertOrUpdate(pindeployBean);
        LOG.info(
                String.format(
                        "Successfully updated pindeploy pipeline for %s/%s by %s",
                        envName, stageName, operator));
    }
}
