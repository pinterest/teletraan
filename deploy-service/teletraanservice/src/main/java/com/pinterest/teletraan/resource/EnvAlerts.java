/**
 * Copyright (c) 2017-2024 Pinterest, Inc.
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

import com.google.common.collect.ImmutableMap;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.alerts.AlertAction;
import com.pinterest.deployservice.alerts.AlertContextBuilder;
import com.pinterest.deployservice.alerts.AutoRollbackAction;
import com.pinterest.deployservice.alerts.DefaultAlertContextBuilder;
import com.pinterest.deployservice.alerts.ExternalAlertFactory;
import com.pinterest.deployservice.alerts.MarkBadBuildAction;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployState;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.EnvironState;
import com.pinterest.deployservice.bean.ExternalAlert;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.DeployHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/alerts")
@Api("ExternalAlerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class EnvAlerts {

    private static final Logger LOG = LoggerFactory.getLogger(EnvWebHooks.class);

    private EnvironDAO environDAO;
    private DeployHandler deployHandler;
    private ExternalAlertFactory externalAlertFactory;
    private ServiceContext serviceContext;
    private Map<String, AlertAction> supportActions;
    private AlertContextBuilder alertContextBuilder;

    public EnvAlerts(@Context TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        externalAlertFactory = context.getExternalAlertsFactory();
        deployHandler = new DeployHandler(context);
        supportActions =
                new ImmutableMap.Builder<String, AlertAction>()
                        .put("rollback", new AutoRollbackAction())
                        .put("markbadbuild", new MarkBadBuildAction())
                        .build();
        serviceContext = context;
        alertContextBuilder = new DefaultAlertContextBuilder();
    }

    public AlertContextBuilder getAlertContextBuilder() {
        return alertContextBuilder;
    }

    public void setAlertContextBuilder(AlertContextBuilder alertContextBuilder) {
        this.alertContextBuilder = alertContextBuilder;
    }

    @POST
    @ApiOperation(
            value = "The alert response",
            notes = "Return the alert checking result",
            response = Response.class)
    @RolesAllowed(TeletraanPrincipalRole.Names.EXECUTE)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.ENV_STAGE,
            idLocation = ResourceAuthZInfo.Location.PATH)
    /**
     * This method is supposed to be triggered by the alerting system from a webhook. It means the
     * environment has received an alert Deploy service will check if there is a recent happened
     * deploy and perform some actions correspondingly.
     */
    public Response alertsTriggered(
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @QueryParam("actionWindow") int actionWindow,
            @QueryParam("actions") String actions,
            @Context SecurityContext sc,
            String alertBody)
            throws Exception {
        EnvironBean environBean = Utils.getEnvStage(environDAO, envName, stageName);
        String userName = sc.getUserPrincipal().getName();
        LOG.info(
                "Get Alert for env {} stage {} actionWindow {} actions {} with alert body {}",
                envName,
                stageName,
                actionWindow,
                actions,
                alertBody);
        ExternalAlert alert = externalAlertFactory.getAlert(alertBody);
        if (alert == null) {
            return Response.status(400).build();
        }

        LOG.info("Get Alert triggered: {} time:{}", alert.isTriggered(), alert.getTriggeredDate());

        if (!alert.isTriggered()) {
            LOG.info("No action for alert off");
            return Response.status(304).build();
        }

        if (alert.getTriggeredDate().plusMinutes(5).isBefore(DateTime.now())) {
            LOG.info("Alert has been delayed too much. Ignore it");
            return Response.status(304).build();
        }
        // Ensure action Window is in a value makes sense.
        if (actionWindow <= 0 || actionWindow > 3600 * 4) {
            // max action window is four hour
            return Response.status(400).entity("actionWindow must be between 0 to 14400").build();
        }

        if (StringUtils.isBlank(environBean.getDeploy_id())) {
            return Response.status(Response.Status.NO_CONTENT).entity("No deploy").build();
        }

        DeployBean lastDeploy = deployHandler.getDeploySafely(environBean.getDeploy_id());

        boolean inWindow =
                DateTime.now().minusSeconds(actionWindow).isBefore(lastDeploy.getStart_date());
        boolean shouldPerformAction =
                environBean.getState() == EnvironState.NORMAL
                        && (lastDeploy.getState() == DeployState.SUCCEEDING
                                || lastDeploy.getState() == DeployState.FAILING);

        Map<String, Object> ret = new HashMap<>();
        if (inWindow && shouldPerformAction) {
            // Take actions
            for (AlertAction action : getActions(actions, supportActions)) {
                try {
                    ret.put(
                            action.getClass().getName(),
                            action.perform(
                                    getAlertContextBuilder().build(serviceContext),
                                    environBean,
                                    lastDeploy,
                                    actionWindow,
                                    userName));
                } catch (Exception ex) {
                    LOG.error(
                            "Failed to perform action {}", ExceptionUtils.getRootCauseMessage(ex));
                    LOG.error(ExceptionUtils.getStackTrace(ex));
                }
            }

        } else if (inWindow) {
            LOG.info(
                    "Don't perform action because environment state is {} and lastDeploy state is {}",
                    environBean.getState(),
                    lastDeploy.getState());
        } else {
            LOG.info("Last deploy is not in window");
        }

        return Response.status(200).entity(ret).build();
    }

    public List<AlertAction> getActions(String actions, Map<String, AlertAction> supportedActions) {
        List<AlertAction> ret = new ArrayList<>();
        String[] tokens = StringUtils.split(actions, " ");
        for (String token : tokens) {
            if (!StringUtils.isBlank(token) && supportedActions.containsKey(token)) {
                ret.add(supportedActions.get(token));
            }
        }
        return ret;
    }
}
