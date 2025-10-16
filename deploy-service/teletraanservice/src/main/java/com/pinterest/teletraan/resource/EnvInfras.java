/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.pinterest.deployservice.bean.*;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.handler.InfraConfigHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\\\-_]+}/infras")
@Api(tags = "Infras")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvInfras {

    private static final Logger LOG = LoggerFactory.getLogger(EnvInfras.class);
    private final InfraConfigHandler infraConfigHandler;

    public EnvInfras(@Context TeletraanServiceContext context) {
        infraConfigHandler = new InfraConfigHandler(context);
    }

    @POST
    @Timed
    @ExceptionMetered
    @ApiOperation(
            value = "Apply infrastructure configurations",
            notes =
                    "Apply infrastructure configurations given an environment name, stage name, and configurations",
            response = Response.class)
    //    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    //    @ResourceAuthZInfo(
    //            type = AuthZResource.Type.ENV_STAGE,
    //            idLocation = ResourceAuthZInfo.Location.PATH)
    public Response apply(
            //            @Context SecurityContext sc,
            @Context UriInfo uriInfo,
            @ApiParam(value = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "Stage name", required = true) @PathParam("stageName")
                    String stageName)
            //            ,
            //  @Valid InfraBean infraBean
            throws Exception {
        //        String operator = sc.getUserPrincipal().getName();

        //        LOG.info(
        //                "No-op endpoint for applying infra configurations was called. envName: {},
        // stageName: {}, clusterName: {}, accountId: {}",
        //                envName,
        //                stageName,
        //                //                operator,
        //                infraBean.getClusterName(),
        //                infraBean.getAccountId());
        //        infraConfigHandler.test(envName, stageName, infraBean.getClusterName());
        //        infraConfigHandler.test(envName, stageName, infraBean);
        return Response.status(200).build();
    }
}
