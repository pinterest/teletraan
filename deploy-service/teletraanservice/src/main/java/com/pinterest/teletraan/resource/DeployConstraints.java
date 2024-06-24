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

import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.DeployConstraintDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.db.DatabaseUtil;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.worker.DeployTagWorker;
import io.swagger.annotations.*;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/deploy_constraint")
@Api(tags = "Deploy Constraints")
@SwaggerDefinition(
        tags = {
            @Tag(name = "Deploy Constraints", description = "Deploy constraints related APIs"),
        })
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeployConstraints {
    private static final Logger LOG = LoggerFactory.getLogger(DeployConstraints.class);

    private DeployConstraintDAO deployConstraintDAO;
    private EnvironDAO environDAO;
    private BasicDataSource dataSource;
    private TeletraanServiceContext serviceContext;

    public DeployConstraints(@Context TeletraanServiceContext context) {
        serviceContext = context;
        deployConstraintDAO = context.getDeployConstraintDAO();
        environDAO = context.getEnvironDAO();
        dataSource = context.getDataSource();
    }

    @GET
    @ApiOperation(
            value = "Get deploy constraint info",
            notes = "Returns a deploy constraint object given a constraint id",
            response = DeployConstraintBean.class)
    public DeployConstraintBean get(
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @Context SecurityContext sc)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        String deployConstraintId = envBean.getDeploy_constraint_id();
        if (deployConstraintId == null) {
            LOG.warn("Environment {} does not have deploy constraint set up.", envBean);
            return null;
        }
        return deployConstraintDAO.getById(deployConstraintId);
    }

    @POST
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.ENV_STAGE,
            idLocation = ResourceAuthZInfo.Location.PATH)
    public void update(
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @ApiParam(value = "Deploy Constraint Object to update in database", required = true)
                    @Valid
                    DeployConstraintBean deployConstraintBean,
            @Context SecurityContext sc)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        String operator = sc.getUserPrincipal().getName();
        String constraintId = envBean.getDeploy_constraint_id();

        DeployConstraintBean updateBean = new DeployConstraintBean();
        Long maxParallel = deployConstraintBean.getMax_parallel();
        if (maxParallel != null && maxParallel > 0) {
            updateBean.setMax_parallel(maxParallel);
        }
        String tagName = deployConstraintBean.getConstraint_key();
        if (tagName != null) {
            updateBean.setConstraint_key(tagName);
        }
        DeployConstraintType type = deployConstraintBean.getConstraint_type();
        if (type != null) {
            updateBean.setConstraint_type(type);
        } else {
            // defaults to ALL_GROUPS_IN_PARALLEL
            updateBean.setConstraint_type(DeployConstraintType.ALL_GROUPS_IN_PARALLEL);
        }

        List<UpdateStatement> statements = new ArrayList<>();
        if (constraintId == null) {
            constraintId = CommonUtils.getBase64UUID();
            updateBean.setConstraint_id(constraintId);
            updateBean.setState(TagSyncState.INIT);
            updateBean.setStart_date(System.currentTimeMillis());
            statements.add(deployConstraintDAO.genInsertStatement(updateBean));

            envBean.setDeploy_constraint_id(constraintId);
            statements.add(environDAO.genUpdateStatement(envBean.getEnv_id(), envBean));
        } else {
            updateBean.setConstraint_id(constraintId);
            updateBean.setState(TagSyncState.INIT);
            updateBean.setStart_date(System.currentTimeMillis());
            statements.add(deployConstraintDAO.genUpdateStatement(constraintId, updateBean));
        }

        DatabaseUtil.transactionalUpdate(dataSource, statements);
        LOG.info(
                "Successfully updated deploy constraint {} for env {}/{} by {}.",
                deployConstraintBean,
                envName,
                stageName,
                operator);

        Runnable worker = new DeployTagWorker(serviceContext);
        worker.run();
        LOG.info(
                "Successfully run DeployTagWorker for env {}/{} by {}.",
                envName,
                stageName,
                operator);
    }

    @DELETE
    @RolesAllowed(TeletraanPrincipalRole.Names.DELETE)
    @ResourceAuthZInfo(
            type = AuthZResource.Type.ENV_STAGE,
            idLocation = ResourceAuthZInfo.Location.PATH)
    public void delete(
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @Context SecurityContext sc)
            throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        String operator = sc.getUserPrincipal().getName();
        String constraintId = envBean.getDeploy_constraint_id();
        if (constraintId == null) {
            LOG.warn("Environment {} does not have deploy constraint set up.", envBean);
        } else {
            envBean.setDeploy_constraint_id(null);
            // remove the link between environ and deploy_constraint
            environDAO.deleteConstraint(envName, stageName);
            // remove the deploy_constraint
            deployConstraintDAO.delete(constraintId);
            LOG.info(
                    "Successfully deleted deploy constraint {} for env {}/{} by {}.",
                    constraintId,
                    envName,
                    stageName,
                    operator);
        }
    }
}
