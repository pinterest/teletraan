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

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PindeployBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.PindeployDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import io.swagger.annotations.*;
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

    public Pindeploy(@Context TeletraanServiceContext context) throws Exception {
        pindeployDAO = context.getPindeployDAO();
        environDAO = context.getEnvironDAO();
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
    public void disablePindeployPipeline(
            @Context SecurityContext sc, @NotEmpty @QueryParam("pipeline") String pipeline)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();
        pindeployDAO.delete(pipeline);
        LOG.info(
                String.format(
                        "Successfully disabled pindeploy pipeline %s by %s", pipeline, operator));
    }

    @POST
    @Path("/enable")
    public void enablePindeployPipeline(
            @Context SecurityContext sc,
            @NotEmpty @QueryParam("envName") String envName,
            @NotEmpty @QueryParam("stageName") String stageName,
            @NotEmpty @QueryParam("pipeline") String pipeline)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
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
