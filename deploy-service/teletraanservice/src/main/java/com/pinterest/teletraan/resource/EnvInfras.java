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

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.bean.rodimus.AsgSummaryBean;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingAlarm;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingPolicies;
import com.pinterest.deployservice.bean.rodimus.RodimusScheduledAction;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.WorkerJobDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\\\-_]+}/infras")
@Api(tags = "Infras")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvInfras {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(EnvInfras.class);

    private WorkerJobDAO workerJobDAO;
    private final EnvironDAO environDAO;
    private final RodimusManager rodimusManager;

    public EnvInfras(@Context TeletraanServiceContext context) {
        workerJobDAO = context.getWorkerJobDAO();
        environDAO = context.getEnvironDAO();
        rodimusManager = context.getRodimusManager();
    }

    @POST
    @Timed
    @Counted
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
                        .job_type(WorkerJobBean.JobType.INFRA_APPLY)
                        .config(
                                mapper.writeValueAsString(
                                        InfraConfigBean.fromInfraBean(
                                                operator, envName, stageName, bean)))
                        .status(WorkerJobBean.Status.INITIALIZED)
                        .create_at(System.currentTimeMillis())
                        .build();

        workerJobDAO.insert(workerJobBean);

        LOG.info("Endpoint for applying infra configurations created a worker job: {}", bean);

        return Response.status(200).entity(workerJobBean).build();
    }

    @GET
    @Timed
    @Counted
    @ApiOperation(
            value = "Get infrastructure configurations",
            notes = "Get infrastructure configurations given environment name and stage name",
            response = InfraBean.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.READ)
    public InfraBean get(
            @Context SecurityContext sc,
            @Context UriInfo uriInfo,
            @ApiParam(value = "Environment name", required = true) @PathParam("envName")
                    String envName,
            @ApiParam(value = "Stage name", required = true) @PathParam("stageName")
                    String stageName)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();

        LOG.info(
                "Endpoint for getting infra configurations was called. envName: {}, stageName: {}, operator: {}",
                envName,
                stageName,
                operator);

        EnvironBean originEnvironBean = Utils.getEnvStage(environDAO, envName, stageName);
        ClusterInfoPublicIdsBean clusterInfoPublicIdsBean =
                rodimusManager.getClusterInfoPublicIdsBean(originEnvironBean.getCluster_name());
        AsgSummaryBean asgSummaryBean =
                rodimusManager.getAutoScalingGroupSummary(originEnvironBean.getCluster_name());
        RodimusAutoScalingPolicies rodimusAutoScalingPolicies =
                rodimusManager.getClusterScalingPolicies(originEnvironBean.getCluster_name());
        List<ScalingPolicyBean> scalingPolicies =
                rodimusAutoScalingPolicies.allSimplePolicies().stream()
                        .map(ScalingPolicyBean::fromRodimusAutoScalingPolicy)
                        .collect(Collectors.toList());

        List<RodimusAutoScalingAlarm> rodimusAutoScalingAlarm =
                rodimusManager.getClusterAlarms(originEnvironBean.getCluster_name());
        List<AutoScalingAlarmBean> autoScalingAlarmBeans =
                rodimusAutoScalingAlarm.stream()
                        .map(AutoScalingAlarmBean::fromRodimusAutoScalingAlarm)
                        .collect(Collectors.toList());

        List<RodimusScheduledAction> rodimusScheduledActions =
                rodimusManager.getClusterScheduledActions(originEnvironBean.getCluster_name());
        List<ScheduledActionBean> scheduledActionBeans =
                rodimusScheduledActions.stream()
                        .map(ScheduledActionBean::fromRodimusScheduledAction)
                        .collect(Collectors.toList());

        InfraBean infraBean =
                InfraBean.builder()
                        .clusterName(originEnvironBean.getCluster_name())
                        .accountId(clusterInfoPublicIdsBean.getAccountId())
                        .region(clusterInfoPublicIdsBean.getRegion())
                        .archName(clusterInfoPublicIdsBean.getArchName())
                        .maxCapacity(asgSummaryBean.getMaxSize())
                        .minCapacity(asgSummaryBean.getMinSize())
                        .provider(clusterInfoPublicIdsBean.getProvider())
                        .baseImage(clusterInfoPublicIdsBean.getBaseImage())
                        .baseImageName(clusterInfoPublicIdsBean.getBaseImageName())
                        .hostType(clusterInfoPublicIdsBean.getHostType())
                        .securityGroup(clusterInfoPublicIdsBean.getSecurityGroup())
                        .subnets(clusterInfoPublicIdsBean.getSubnets())
                        .configs(clusterInfoPublicIdsBean.getConfigs())
                        .autoUpdateBaseImage(clusterInfoPublicIdsBean.getAutoUpdateBaseImage())
                        .statefulStatus(clusterInfoPublicIdsBean.getStatefulStatus())
                        .autoRefresh(clusterInfoPublicIdsBean.getAutoRefresh())
                        .replacementTimeout(clusterInfoPublicIdsBean.getReplacementTimeout())
                        .useEnaExpress(clusterInfoPublicIdsBean.getUseEnaExpress())
                        .useEbsCheck(clusterInfoPublicIdsBean.getUseEbsCheck())
                        .scalingPolicies(scalingPolicies)
                        .autoScalingAlarms(autoScalingAlarmBeans)
                        .scheduledActions(scheduledActionBeans)
                        .build();

        LOG.info("Endpoint for getting infra configurations found configurations: {}", infraBean);

        return infraBean;
    }
}
