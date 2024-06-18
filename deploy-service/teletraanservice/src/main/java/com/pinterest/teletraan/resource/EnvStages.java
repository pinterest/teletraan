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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvTagHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.deployservice.handler.TagHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo.Location;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RolesAllowed({TeletraanPrincipalRole.Names.READ, TeletraanPrincipalRole.Names.READER})
@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}")
@Api(tags = "Environments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvStages {
    public enum ActionType {
        ENABLE,
        DISABLE
    }

    private static final Logger LOG = LoggerFactory.getLogger(EnvStages.class);
    private EnvironDAO environDAO;
    private EnvironHandler environHandler;
    private ConfigHistoryHandler configHistoryHandler;
    private TagHandler tagHandler;


    public EnvStages(@Context TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        environHandler = new EnvironHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
        tagHandler = new EnvTagHandler(context);
    }

    @GET
    @ApiOperation(
            value = "Get an environment",
            notes = "Returns an environment object given environment and stage names",
            response = EnvironBean.class)
    public EnvironBean get(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName) throws Exception {
        return Utils.getEnvStage(environDAO, envName, stageName);
    }

    @PUT
    @ApiOperation(
            value = "Update an environment",
            notes = "Update an environment given environment and stage names with a environment object")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = Location.PATH)
    public void update(@Context SecurityContext sc,
                       @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
                       @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
                       @ApiParam(value = "Desired Environment object with updates", required = true)
                           EnvironBean environBean) throws Exception {
        final EnvironBean origBean = Utils.getEnvStage(environDAO, envName, stageName);
        // treat null as false
        boolean originalIsSox = origBean.getIs_sox() != null && origBean.getIs_sox();

        if (environBean.getIs_sox() == null) {
            environBean.setIs_sox(originalIsSox);
        } else if (!environBean.getIs_sox().equals(originalIsSox)) {
            throw new WebApplicationException("Modification of isSox flag is not allowed!", Response.Status.FORBIDDEN);
        }

        String operator = sc.getUserPrincipal().getName();
        try {
            environBean.validate();
            stageTypeValidate(origBean, environBean);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.toString(), Response.Status.BAD_REQUEST);
        }

        if (environBean.getStage_type() == EnvType.DEV) {
            environBean.setAllow_private_build(true);
        }
        environBean.setEnv_name(origBean.getEnv_name());
        environBean.setStage_name(origBean.getStage_name());
        if (environBean.getExternal_id() == null) {
            environBean.setExternal_id(origBean.getExternal_id());
        }
        environHandler.updateStage(environBean, operator);
        configHistoryHandler.updateConfigHistory(origBean.getEnv_id(), Constants.TYPE_ENV_GENERAL, environBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, origBean.getEnv_id(), Constants.TYPE_ENV_GENERAL, operator, environBean.getExternal_id());
        LOG.info("Successfully updated env {}/{} with {} by {}.",
            envName, stageName, environBean, operator);
    }

    @PUT
    @Path("/is-sox/{booleanValue}")
    @ApiOperation(
            value = "Update an environment/stage's isSox flag"
    )
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.SYSTEM, idLocation = Location.PATH)
    public void updateIsSox(@Context SecurityContext sc,
                       @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
                       @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
                       @ApiParam(value = "Is sox flag", required = true)@PathParam("booleanValue") boolean isSox) throws Exception {
        EnvironBean origBean = Utils.getEnvStage(environDAO, envName, stageName);
        origBean.setIs_sox(isSox);
        String operator = sc.getUserPrincipal().getName();
        environHandler.updateStage(origBean, operator);
        configHistoryHandler.updateConfigHistory(origBean.getEnv_id(), Constants.TYPE_ENV_GENERAL, origBean, operator);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, origBean.getEnv_id(), Constants.TYPE_ENV_GENERAL, operator, origBean.getExternal_id());
        LOG.info("Successfully updated env {}/{} isSox flag to {} by {}.", envName, stageName, isSox, operator);
    }

    @DELETE
    @ApiOperation(
            value = "Delete an environment",
            notes = "Deletes an environment given a environment and stage names")
    @RolesAllowed(TeletraanPrincipalRole.Names.DELETE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = Location.PATH)
    public void delete(@Context SecurityContext sc,
                       @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
                       @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName)
                        throws Exception {
        String operator = sc.getUserPrincipal().getName();
        environHandler.deleteEnvStage(envName, stageName, operator);
        LOG.info("Successfully deleted env {}/{} by {}.", envName, stageName, operator);
    }

    @POST
    @ApiOperation(
          value = "Sets the external_id on a stage",
          notes = "Sets the external_id column on a stage given the environment and stage names",
          response = EnvironBean.class
    )
    @Path("/external_id")
    @RolesAllowed(TeletraanPrincipalRole.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = ResourceAuthZInfo.Location.PATH)
    public EnvironBean setExternalId(
            @ApiParam(value = "Environment name", required = true)@PathParam("envName") String envName,
            @ApiParam(value = "Stage name", required = true)@PathParam("stageName") String stageName,
            @ApiParam(value="External id", required = true) String externalId)
            throws Exception {

       try {
         UUID uuid = UUID.fromString(externalId);
       } catch (Exception ex){
         LOG.info("Invalid UUID supplied - {}.", externalId);
         throw new WebApplicationException(String.format(
                 "Client supplied an invalid externalId - %s. Please retry with an externalId in the UUID format",
                 externalId), Response.Status.BAD_REQUEST);
       }

       EnvironBean originalBean = environDAO.getByStage(envName, stageName);
       if(originalBean == null) {
           throw new WebApplicationException(String.format("Environment %s/%s does not exist.", envName, stageName),
                   Response.Status.NOT_FOUND);
       }
       environDAO.setExternalId(originalBean, externalId);
       EnvironBean updatedBean = environDAO.getByStage(envName, stageName);
       String newExternalId = updatedBean.getExternal_id();

       LOG.info("Successfully updated Env/stage - {}/{} with externalid = {}", envName, stageName, newExternalId);
       return updatedBean;
    }

    @POST
    @Path("/actions")
    @RolesAllowed(TeletraanPrincipalRole.Names.EXECUTE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = Location.PATH)
    public void action(@Context SecurityContext sc,
                       @PathParam("envName") String envName,
                       @PathParam("stageName") String stageName,
                       @NotNull @QueryParam("actionType") ActionType actionType,
                       @NotEmpty @QueryParam("description") String description) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        String operator = sc.getUserPrincipal().getName();

        TagBean tagBean = new TagBean();
        switch (actionType) {
            case ENABLE:
                environHandler.enable(envBean, operator);
                tagBean.setValue(TagValue.ENABLE_ENV);
                break;
            case DISABLE:
                environHandler.disable(envBean, operator);
                tagBean.setValue(TagValue.DISABLE_ENV);
                break;
            default:
                throw new WebApplicationException("No action found.", Response.Status.BAD_REQUEST);
        }

        tagBean.setTarget_id(envBean.getEnv_id());
        tagBean.setTarget_type(TagTargetType.ENVIRONMENT);
        tagBean.setComments(description);
        tagHandler.createTag(tagBean, operator);
        LOG.info(String.format("Successfully updated action %s for %s/%s by %s", actionType, envName, stageName, operator));
    }

    private void stageTypeValidate(EnvironBean origBean, EnvironBean newBean) throws Exception {
        Map<EnvType, String> stageTypeCategory = new HashMap<>();
        stageTypeCategory.put(EnvType.DEFAULT, "PRODUCTION");
        stageTypeCategory.put(EnvType.PRODUCTION, "PRODUCTION");
        stageTypeCategory.put(EnvType.CONTROL, "PRODUCTION");
        stageTypeCategory.put(EnvType.CANARY, "PRODUCTION");
        stageTypeCategory.put(EnvType.STAGING, "NON-PRODUCTION");
        stageTypeCategory.put(EnvType.LATEST, "NON-PRODUCTION");
        stageTypeCategory.put(EnvType.DEV, "NON-PRODUCTION");

        if (origBean.getStage_type() == EnvType.DEFAULT && newBean.getStage_type() == null) {
            throw new IllegalArgumentException("Please update the Stage Type to a value other than DEFAULT.");
        } else if (newBean.getStage_type() == null) {
            // Request has no intention to change stage type, so set it to the current value
            // to avoid the default value being used.
            newBean.setStage_type(origBean.getStage_type());
        } else if (origBean.getStage_type() != EnvType.DEFAULT
                && origBean.getStage_type() != newBean.getStage_type()
                && stageTypeCategory.get(newBean.getStage_type()).equals("NON-PRODUCTION")
                && stageTypeCategory.get(origBean.getStage_type()).equals("PRODUCTION")) {
            throw new IllegalArgumentException(
                "Modification of Production stage type (PRODUCTION, CANARY, CONTROL) is not allowed!");
        }
    }
}
