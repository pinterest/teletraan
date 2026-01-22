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

import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.WorkerJobBean;
import com.pinterest.deployservice.dao.WorkerJobDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs/infras/job")
@Api(tags = "Infras")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvInfrasJob {

    private static final Logger LOG = LoggerFactory.getLogger(EnvInfrasJob.class);

    private WorkerJobDAO workerJobDAO;

    public EnvInfrasJob(@Context TeletraanServiceContext context) {
        workerJobDAO = context.getWorkerJobDAO();
    }

    @GET
    @Path(
            "/{jobId : [0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}}")
    @Timed
    @Counted
    @ApiOperation(
            value = "Get status of applying infrastructure configurations",
            notes = "Get status of applying infrastructure configurations given a job id",
            response = Response.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.READ)
    public Response getJob(
            @Context SecurityContext sc,
            @Context UriInfo uriInfo,
            @ApiParam(value = "Job id", required = true) @PathParam("jobId") String jobId)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();

        LOG.info(
                "Endpoint for getting status of applying infra configurations was called. jobId: {}, operator: {}",
                jobId,
                operator);

        WorkerJobBean workerJobBean = workerJobDAO.getById(jobId);

        if (workerJobBean == null) {
            LOG.info(
                    "Endpoint for getting status of applying infra configurations did not find jobId: {}",
                    jobId);
            return Response.status(404).build();
        }

        LOG.info(
                "Endpoint for getting status of applying infra configurations found job: {}",
                workerJobBean);

        return Response.status(200).entity(workerJobBean).build();
    }
}
