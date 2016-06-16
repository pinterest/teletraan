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


import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.autoscaling.AwsAutoScalingManager;
import com.pinterest.arcee.bean.*;
import com.pinterest.arcee.dao.GroupMappingDAO;
import com.pinterest.arcee.dao.PasConfigDAO;
import com.pinterest.arcee.handler.GroupHandler;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.clusterservice.cm.ClusterManager;
import com.pinterest.clusterservice.cm.AwsVmManager;
import com.pinterest.clusterservice.cm.ClusterManager;
import com.pinterest.clusterservice.cm.AwsVmManager;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.*;


@Path("/v1/groups/{groupName: [a-zA-Z0-9\\-_]+}/autoscaling")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AutoScalingGroups {
    public enum AutoScalingActionType {
        ENABLE,
        DISABLE
    }

    public enum InstancesActionType {
        ATTACH,
        DETACH,
        LAUNCH,
        TERMINATE,
        PROTECT,
        UNPROTECT,
    }

    private static final Logger LOG = LoggerFactory.getLogger(Groups.class);
    private EnvironDAO environDAO;
    private GroupHandler groupHandler;
    private ConfigHistoryHandler configHistoryHandler;
    private final Authorizer authorizer;
    private PasConfigDAO pasConfigDAO;
    private GroupMappingDAO groupMappingDAO;
    private ClusterManager clusterManager;
    private AutoScalingManager autoScalingManager;

    public AutoScalingGroups(TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        groupHandler = new GroupHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
        authorizer = context.getAuthorizer();
        pasConfigDAO = context.getPasConfigDAO();
        groupMappingDAO = context.getGroupMappingDAO();
        clusterManager = context.getClusterManager();
        autoScalingManager = context.getAutoScalingManager();
        groupMappingDAO = context.getGroupMappingDAO();
        clusterManager = context.getClusterManager();
        autoScalingManager = context.getAutoScalingManager();
    }

    @POST
    public void createAutoScalingGroup(@Context SecurityContext sc,
                                       @PathParam("groupName") String groupName,
                                       @Valid AwsVmBean request) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.createAutoScalingGroup(groupName, request);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_SCALING, request, operator);
        LOG.info("Successfully created auto scaling on group {}", groupName);
    }

    @DELETE
    public void deleteAutoScalingGroup(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        @QueryParam("detach_instances") boolean detachInstance) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.deleteAutoScalingGroup(groupName, detachInstance);
        String configChange = String.format("Delete Auto Scaling. Detach Instance: %b", detachInstance);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_SCALING, configChange, operator);
        LOG.info("Deleting auto scaling group {}, and detach instance is: {}", groupName, detachInstance);
    }

    @GET
    public List<AutoScalingGroupBean> getAutoScalingGroupInfoByName(
        @PathParam("groupName") String groupName) throws Exception {
        return groupHandler.getAutoScalingGroupInfoByName(groupName);
    }

    @GET
    @Path("/summary")
    public AutoScalingSummaryBean getAutoScalingSummaryByName(@PathParam("groupName") String groupName) throws Exception {
        return groupHandler.getAutoScalingSummaryByName(groupName);
    }

    @PUT
    public void updateAutoScalingGroup(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        AutoScalingRequestBean request) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.updateAutoScalingGroup(groupName, request);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_SCALING, request, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_GROUP, groupName, Constants.TYPE_ASG_SCALING, operator);
        LOG.info("Updated auto scaling group {}", groupName);
    }

    @POST
    @Path("/action")
    public void actionOnAutoScalingGroup(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        @NotEmpty @QueryParam("type") String type) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        AutoScalingActionType actionType = AutoScalingActionType.valueOf(AutoScalingActionType.class, type.toUpperCase());
        String operator = sc.getUserPrincipal().getName();
        if (actionType == AutoScalingActionType.ENABLE) {
            groupHandler.enableAutoScalingGroup(groupName);
            String configChange = String.format("Enable Auto Scaling");
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_SCALING_ACTION, configChange, operator);
            LOG.info("Successfully enabled auto scaling on group {}", groupName);
            return;
        } else if (actionType == AutoScalingActionType.DISABLE) {
            groupHandler.disableAutoScalingGroup(groupName);
            String configChange = String.format("Disable Auto Scaling");
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_SCALING_ACTION, configChange, operator);
            LOG.info("Successfully disabled auto scaling on group {}", groupName);
            return;
        }

        throw new TeletaanInternalException(Response.Status.BAD_REQUEST, String.format("Unknow action type:%s", type));
    }

    @GET
    @Path("/status")
    public ASGStatus getAutoScalingGroupStatus(
        @PathParam("groupName") String groupName) throws Exception {
        return groupHandler.getAutoScalingGroupStatus(groupName);
    }


    @GET
    @Path("/policies")
    public ScalingPoliciesBean getScalingPolicyInfoByName(
        @PathParam("groupName") String groupName) throws Exception {
        return groupHandler.getScalingPolicyInfoByName(groupName);
    }

    @POST
    @Path("/policies")
    public void putScalingPolicyToGroup(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        ScalingPoliciesBean request) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.putScalingPolicyToGroup(groupName, request);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_POLICY, request, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_GROUP, groupName, Constants.TYPE_ASG_POLICY, operator);
        LOG.info("Put scaling policy to group {}", groupName);
    }

    @PUT
    @Path("/alarms")
    public void updateAlarmsToAutoScalingGroup(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        List<AsgAlarmBean> request) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.updateAlarmsToAutoScalingGroup(groupName, request);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_ALARM, request, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_GROUP, groupName, Constants.TYPE_ASG_ALARM, operator);
        LOG.info("{} updated alarms {} to group {}", operator, request, groupName);
    }

    @POST
    @Path("/alarms")
    public void addAlarmsToAutoScalingGroup(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        List<AsgAlarmBean> alarmInfos) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.addAlarmsToAutoScalingGroup(groupName, alarmInfos);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_ALARM, alarmInfos, operator);
        LOG.info("Successfully added new alarms {} to group {}", alarmInfos, groupName);
    }

    @GET
    @Path("/alarms")
    public List<AsgAlarmBean> getAlarmInfoByGroup(@PathParam("groupName") String groupName) throws Exception {
        return groupHandler.getAlarmInfoByGroup(groupName);
    }

    @DELETE
    @Path(("/alarms/{alarm_id: [a-zA-Z0-9\\-_]+}"))
    public void deleteAlarmFromAutoScalingGroup(@Context SecurityContext sc,
        @PathParam("groupName") String groupName,
        @PathParam("alarm_id") String alarmId) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        groupHandler.deleteAlarmFromAutoScalingGroup(alarmId);
        LOG.info("deleted new alarms to group {}", groupName);
    }

    @GET
    @Path("/metrics/system")
    public List<String> listAwsMetricNames(@PathParam("groupName") String groupName) throws Exception {
        return groupHandler.listAwsMetricNames(groupName);
    }

    @GET
    @Path("/activities")
    public ScalingActivitiesBean getScalingActivities(@PathParam("groupName") String groupName,
        @QueryParam("size") int pageSize, @QueryParam("token") String token) throws Exception {
        return groupHandler.getScalingActivities(groupName, pageSize, token);
    }

    @POST
    @Path("/instances/action")
    public void attachInstanceToAutoScalingGroup(@Context SecurityContext sc,
        @Valid List<String> instanceIds,
        @PathParam("groupName") String groupName,
        @QueryParam("type") String type) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        InstancesActionType actionType = InstancesActionType.valueOf(InstancesActionType.class, type.toUpperCase());
        String operator = sc.getUserPrincipal().getName();
        if (actionType == InstancesActionType.ATTACH) {
            groupHandler.attachInstanceToAutoScalingGroup(instanceIds, groupName);
            String configChange = String.format("Attached instances %s to the group %s. Increased capacity by %d.", instanceIds.toString(), groupName, instanceIds.size());
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_HOST_ATTACH, configChange, operator);
            LOG.info(configChange);
            return;
        } else if (actionType == InstancesActionType.DETACH) {
            groupHandler.detachInstanceFromAutoScalingGroup(instanceIds, groupName);
            String configChange = String.format("Detached instances %s to the group %s. Decreased capacity by %d.", instanceIds.toString(), groupName, instanceIds.size());
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_HOST_DETACH, configChange, operator);
            LOG.info(configChange);
            return;
        } else if (actionType == InstancesActionType.PROTECT) {
            groupHandler.protectInstancesInAutoScalingGroup(instanceIds, groupName);
            String configChange = String.format("Protect instances %s from terminating in group %s.", instanceIds.toString(), groupName);
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_HOST_PROTECTION, configChange, operator);
            return;
        } else if (actionType == InstancesActionType.UNPROTECT) {
            groupHandler.unprotectInstancesInAutoScalingGroup(instanceIds, groupName);
            String configChange = String.format("Unprotect instances %s from terminating in group %s.", instanceIds.toString(), groupName);
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_HOST_UNPROTECTION, configChange, operator);
            return;
        }

        throw new TeletaanInternalException(Response.Status.BAD_REQUEST, String.format("Unknown action type: %s", type));
    }

    @GET
    @Path("/instance/protection")
    public Boolean instanceProtection(@Context SecurityContext sc, @Valid List<String> hostIds,
                                     @PathParam("groupName") String groupName,
                                     @QueryParam("name") String name) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        return groupHandler.isInstanceProtected(groupName, hostIds.get(0));
    }

    @GET
    @Path("/instances")
    public List<String> getAutoScalingGroupHosts(@PathParam("groupName") String groupName) throws Exception {
        List<AutoScalingGroupBean> autoScalingGroupBeans = groupHandler.getAutoScalingGroupInfoByName(groupName);
        List<String> instances = new ArrayList<>();
        for (AutoScalingGroupBean autoScalingGroupBean : autoScalingGroupBeans) {
            instances.addAll(autoScalingGroupBean.getInstances());
        }
        return instances;
    }

    @GET
    @Path("/event/scalingdown")
    public boolean getASGTerminateEventStatus(
        @PathParam("groupName") String groupName) throws Exception {
        return groupHandler.isScalingDownEventEnabled(groupName);
    }

    @POST
    @Path("/event/scalingdown/action")
    public void actionOnScalingDownEvent(@Context SecurityContext sc,
                                         @PathParam("groupName") String groupName,
                                         @NotEmpty @QueryParam("type") String type) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        AutoScalingActionType actionType = AutoScalingActionType.valueOf(AutoScalingActionType.class, type.toUpperCase());
        String operator = sc.getUserPrincipal().getName();
        if (actionType == AutoScalingActionType.ENABLE) {
            groupHandler.enableScalingDownEvent(groupName);
            String configChange = "Enable Scaling Down event";
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_SCALING_ACTION, configChange, operator);
            LOG.info("Successfully enabled scaling down on group {}", groupName);
            return;
        } else if (actionType == AutoScalingActionType.DISABLE) {
            groupHandler.disableScalingDownEvent(groupName);
            String configChange = "Disable Scaling Down event";
            configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_SCALING_ACTION, configChange, operator);
            LOG.info("Successfully disabled scaling down on group {}", groupName);
            return;
        }

        throw new TeletaanInternalException(Response.Status.BAD_REQUEST, String.format("Unknow action type:%s", type));
    }

    @POST
    @Path("/pas")
    public void updatePasConfig(@Context SecurityContext sc,
                                @PathParam("groupName") String groupName,
                                @Valid PasConfigBean request) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        pasConfigDAO.updatePasConfig(request);
    }

    @GET
    @Path("/pas")
    public HashMap<String, Object> getPasConfig(@Context SecurityContext sc,
                                      @PathParam("groupName") String groupName) throws Exception {
        PasConfigBean config = pasConfigDAO.getPasConfig(groupName);
        AutoScalingGroup group = autoScalingManager.getAutoScalingGroup(groupName);
        Map<String, ScalingPolicyBean> scalingPolicyBeans = autoScalingManager.getScalingPoliciesForGroup(groupName);
        HashMap<String, Object> pasConfig = new HashMap<>();

        if (scalingPolicyBeans.isEmpty()) {
            return pasConfig;
        }

        // Combine PasConfigBean data and data from AWS
        pasConfig.put("group_name", groupName);
        pasConfig.put("throughput", config.getThroughput());
        pasConfig.put("metric", config.getMetric());
        pasConfig.put("pas_state", config.getPas_state());
        pasConfig.put("last_updated", config.getLast_updated());
        pasConfig.put("max_size", group.getMaxSize());
        pasConfig.put("min_size", group.getMinSize());
        // SCALEUP and SCALEDOWN cooldown times are the same
        pasConfig.put("cooldown", scalingPolicyBeans.get(PolicyType.SCALEUP.toString()).getCoolDownTime());
        return pasConfig;
    }

    @GET
    @Path("/pas/configs")
    public List<String> getAllPasGroups(@PathParam("groupName") String groupName) throws Exception {
        return pasConfigDAO.getAllPasGroups();
    }

    @GET
    @Path("/pas/configs/enabled")
    public List<PasConfigBean> getEnabledPasConfigs(@PathParam("groupName") String groupName) throws Exception {
        return pasConfigDAO.getEnabledPasConfigs();
    }


    @POST
    @Path("/schedules")
    public void putScheduledActionsToAutoScalingGroup(@Context SecurityContext sc,
                                                      @PathParam("groupName") String groupName,
                                                      @Valid Collection<AsgScheduleBean> asgScheduleBeans) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        String operator = sc.getUserPrincipal().getName();
        groupHandler.putScheduledActionsToAutoScalingGroup(groupName, asgScheduleBeans);
        configHistoryHandler.updateConfigHistory(groupName, Constants.TYPE_ASG_SCHEDULE, asgScheduleBeans, operator);
        LOG.info(String.format("Successfully added scheduled actions %s to group %s", asgScheduleBeans, groupName));
    }

    @GET
    @Path("/schedules")
    public Collection<AsgScheduleBean> getScheduledActionsByAutoScalingGroup(@PathParam("groupName") String groupName) throws Exception {
        return groupHandler.getScheduledActionsByAutoScalingGroup(groupName);
    }

    @DELETE
    @Path(("/schedules/{actionId: [a-zA-Z0-9\\-_]+}"))
    public void deleteScheduledActionFromAutoScalingGroup(@Context SecurityContext sc,
                                                          @PathParam("groupName") String groupName,
                                                          @PathParam("actionId") String actionId) throws Exception {
        Utils.authorizeGroup(environDAO, groupName, sc, authorizer, Role.OPERATOR);
        groupHandler.deleteScheduledActionFromAutoScalingGroup(groupName, actionId);
        LOG.info(String.format("Successfully deleted scheduled action %s for group %s", actionId, groupName));
    }

    @GET
    @Path("/spot_instances")
    public Collection<String> getSpotInstancesByGroupName(@Context SecurityContext sc,
                                                    @PathParam("groupName") String groupName,
                                                    @Valid List<String> instanceIds) throws Exception {
        Collection<GroupMappingBean> groupMappingBeans = groupMappingDAO.getGroupMappingsByCluster(groupName);
        HashSet<String> groups = new HashSet<>();
        for (GroupMappingBean groupMappingBean : groupMappingBeans) {
            groups.add(groupMappingBean.getAsg_group_name());
        }
        return clusterManager.getHosts(groups, instanceIds);
    }
}
