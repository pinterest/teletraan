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

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import com.google.common.base.Optional;

import com.pinterest.deployservice.bean.AcceptanceStatus;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployFilterBean;
import com.pinterest.deployservice.bean.DeployQueryResultBean;
import com.pinterest.deployservice.bean.DeployState;
import com.pinterest.deployservice.bean.DeployType;
import com.pinterest.deployservice.bean.TeletraanPrincipalRoles;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.db.DeployQueryFilter;
import com.pinterest.deployservice.handler.DeployHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.deployservice.exception.TeletaanInternalException;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo.Location;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@PermitAll
@Path("/v1/deploys")
@Api(tags = "Deploys")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Deploys", description = "Deploy info APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Deploys {
    private static final Logger LOG = LoggerFactory.getLogger(Deploys.class);
    private static final int DEFAULT_SIZE = 30;
    private DeployDAO deployDAO;
    private DeployHandler deployHandler;

    public Deploys(@Context TeletraanServiceContext context) {
        deployDAO = context.getDeployDAO();
        deployHandler = new DeployHandler(context);
    }

    @GET
    @Timed
    @ExceptionMetered
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get deploy info",
            notes = "Returns a deploy object given a deploy id",
            response = DeployBean.class)
    public DeployBean get(
            @ApiParam(value = "Deploy id", required = true)@PathParam("id") String id) throws Exception {
        DeployBean deployBean = deployDAO.getById(id);
        if (deployBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND,
                String.format("Deploy %s does not exist.", id));
        }
        return deployBean;
    }

    @GET
    public DeployQueryResultBean search(
        @QueryParam("envId") List<String> envIds,
        @QueryParam("operator") List<String> operators,
        @QueryParam("deployType") List<DeployType> deployTypes,
        @QueryParam("deployState") List<DeployState> deployStates,
        @QueryParam("acceptanceStatus") List<AcceptanceStatus> acceptanceStatuss,
        @QueryParam("commit") String commit,
        @QueryParam("repo") String repo,
        @QueryParam("branch") String branch,
        @QueryParam("commitDate") Long commitDate,
        @QueryParam("before") Long before,
        @QueryParam("after") Long after,
        @QueryParam("pageIndex") Optional<Integer> pageIndex,
        @QueryParam("pageSize") Optional<Integer> pageSize,
        @QueryParam("oldestFirst") Optional<Boolean> oldestFirst)
        throws Exception {
        DeployFilterBean filter = new DeployFilterBean();
        filter.setEnvIds(envIds);
        filter.setOperators(operators);
        filter.setDeployTypes(deployTypes);
        filter.setDeployStates(deployStates);
        filter.setAcceptanceStatuss(acceptanceStatuss);
        filter.setCommit(commit);
        filter.setRepo(repo);
        filter.setBranch(branch);
        filter.setCommitDate(commitDate);
        filter.setBefore(before);
        filter.setAfter(after);
        filter.setPageIndex(pageIndex.or(1));
        filter.setPageSize(pageSize.or(DEFAULT_SIZE));
        filter.setOldestFirst(oldestFirst.or(false));
        DeployQueryFilter filterBean = new DeployQueryFilter(filter);
        return deployDAO.getAllDeploys(filterBean);
    }

    @PUT
    @Timed
    @ExceptionMetered
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Update deploy",
            notes = "Update deploy given a deploy id and a deploy object. Current only "
                    + "acceptanceStatus and description are allowed to change.")
    @RolesAllowed(TeletraanPrincipalRoles.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = Location.BODY, beanClass = DeployBean.class)
    public void update(
            @Context SecurityContext sc,
            @ApiParam(value = "Deploy id", required = true)@PathParam("id") String id,
            @ApiParam(value = "Partially populated deploy object", required = true)
            DeployBean deployBean) throws Exception {
        String userName = sc.getUserPrincipal().getName();
        deployHandler.update(id, deployBean);
        LOG.info("{} successfully updated deploy {} with {}",
            userName, id, deployBean);
    }

    @DELETE
    @Timed
    @ExceptionMetered
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Delete deploy info",
            notes = "Delete deploy info given a deploy id")
    @RolesAllowed(TeletraanPrincipalRoles.Names.WRITE)
    @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = Location.BODY, beanClass = DeployBean.class)
    public void delete(
            @Context SecurityContext sc,
            @ApiParam(value = "Deploy id", required = true)@PathParam("id") String id) throws Exception {
        String userName = sc.getUserPrincipal().getName();
        deployDAO.delete(id);
        LOG.info("Successfully deleted deploy {} by {}", id, userName);
    }

    @GET
    @Path("/dailycount")
    @ApiOperation(
            value = "Get deploys per day",
            notes = "Get total numbers of deploys on the current day",
            response = Long.class)
    public long dailyCount() throws Exception{
        return deployDAO.getDailyDeployCount();
    }
}
