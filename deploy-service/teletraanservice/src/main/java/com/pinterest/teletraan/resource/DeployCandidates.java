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

import com.pinterest.deployservice.bean.DeployCandidatesResponse;
import com.pinterest.deployservice.bean.PingRequestBean;
import com.pinterest.deployservice.bean.PingResponseBean;
import com.pinterest.deployservice.bean.PingResult;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.handler.GoalAnalyst;
import com.pinterest.deployservice.handler.PingHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/system")
@Api(tags = "Hosts and Systems")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeployCandidates {
    private static final Logger LOG = LoggerFactory.getLogger(DeployCandidates.class);
    private PingHandler pingHandler;

    public DeployCandidates(@Context TeletraanServiceContext context) {
        pingHandler = new PingHandler(context);
    }

    @POST
    @Path("/ping/alldeploycandidates")
    @ApiOperation(
            value = "Get a set of deploy candidates to deploy",
            notes = "Returns a list of build bean",
            response = DeployCandidatesResponse.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.PINGER)
    @ResourceAuthZInfo(type = AuthZResource.Type.SYSTEM)
    public DeployCandidatesResponse getDeployCandidates(
            @Context SecurityContext sc,
            @Context HttpHeaders headers,
            @ApiParam(value = "Ping request object", required = true) @Valid
                    PingRequestBean requestBean)
            throws Exception {
        LOG.info("Receive ping request " + requestBean);
        boolean rate_limited =
                Boolean.parseBoolean(headers.getRequestHeaders().getFirst("x-envoy-low-watermark"));
        PingResult result = pingHandler.ping(requestBean, rate_limited);
        DeployCandidatesResponse resp = new DeployCandidatesResponse();
        if (result.getInstallCandidates() != null) {
            for (GoalAnalyst.InstallCandidate candidate : result.getInstallCandidates()) {
                PingResponseBean pingResponse = pingHandler.generateInstallResponse(candidate);
                if (pingResponse != null) {
                    if (pingResponse.getDeployGoal().getBuild() == null
                            && pingResponse.getDeployGoal().getDeployId() != null) {
                        // By default, build is not filled unless in downloading stage
                        pingHandler.fillBuildForDeployGoal(pingResponse.getDeployGoal());
                    }
                    resp.getCandidates().add(pingResponse);
                }
            }
        }
        LOG.info("Send {} buildCandidates ", resp.getCandidates().size());
        return resp;
    }
}
