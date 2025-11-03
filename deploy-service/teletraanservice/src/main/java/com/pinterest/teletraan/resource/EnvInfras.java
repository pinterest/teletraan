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
import com.pinterest.deployservice.dao.WorkerJobDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs")
@Api(tags = "Infras")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvInfras {

    private static final Logger LOG = LoggerFactory.getLogger(EnvInfras.class);

    private WorkerJobDAO workerJobDAO;

    public EnvInfras(@Context TeletraanServiceContext context) {
        workerJobDAO = context.getWorkerJobDAO();
    }

    @POST
    @Path("/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\\\-_]+}/infras")
    @Timed
    @ExceptionMetered
    @ApiOperation(
            value = "Apply infrastructure configurations",
            notes =
                    "Apply infrastructure configurations given an environment name, stage name, and configurations",
            response = Response.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.EXECUTE)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.ENV_STAGE,
            idLocation = ResourceAuthZInfo.Location.PATH)
    public Response apply(
            @Context SecurityContext sc,
            @Context UriInfo uriInfo,
            @ApiParam(value = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "Stage name", required = true) @PathParam("stageName")
                    String stageName,
            @Valid InfraBean bean)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();

        LOG.info(
                "Endpoint for applying infra configurations was called. envName: {}, stageName: {}, operator: {}, bean: {}",
                envName,
                stageName,
                operator,
                bean);

        String jobId = UUID.randomUUID().toString();
        WorkerJobBean workerJobBean =
                WorkerJobBean.builder()
                        .id(jobId)
                        .jobType(WorkerJobBean.JobType.INFRA_APPLY)
                        .config(bean)
                        .status(WorkerJobBean.Status.INITIALIZED)
                        .createAt(System.currentTimeMillis())
                        .build();

        workerJobDAO.insert(workerJobBean);

        LOG.info("Endpoint for applying infra configurations created a worker job: {}", bean);

        return Response.status(200).entity(workerJobBean).build();
    }

    @GET
    @Path(
            "/v1/envs/infras/{jobId : [0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}}")
    @Timed
    @ExceptionMetered
    @ApiOperation(
            value = "Get status of applying infrastructure configurations",
            notes = "Get status of applying infrastructure configurations given a job id",
            response = Response.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.READ)
    public Response getJobStatus(
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
            return Response.status(400).build();
        }

        LOG.info(
                "Endpoint for getting status of applying infra configurations found job: {}",
                workerJobBean);

        return Response.status(200).entity(workerJobBean).build();
    }
}
