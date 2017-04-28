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

import com.google.common.base.Optional;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.buildtags.BuildTagsManagerImpl;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.scm.SourceControlManager;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/v1/builds")
@Api(tags = "Builds")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Builds", description = "BUILD information APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Builds {
    private static final Logger LOG = LoggerFactory.getLogger(Builds.class);
    private final static int DEFAULT_SIZE = 100;
    private BuildDAO buildDAO;
    private TagDAO tagDAO;
    private SourceControlManager sourceControlManager;
    private final Authorizer authorizer;

    @Context
    UriInfo uriInfo;

    public Builds(TeletraanServiceContext context) throws Exception {
        buildDAO = context.getBuildDAO();
        tagDAO = context.getTagDAO();
        sourceControlManager = context.getSourceControlManager();
        authorizer = context.getAuthorizer();
    }

    @GET
    @Path("/names")
    public List<String> getBuildNames(@QueryParam("filter") Optional<String> nameFilter,
        @QueryParam("start") Optional<Integer> start, @QueryParam("size") Optional<Integer> size) throws Exception {
        return buildDAO.getBuildNames(nameFilter.orNull(), start.or(1), size.or(DEFAULT_SIZE));
    }

    @GET
    @Path("/names/{name : [a-zA-Z0-9\\-_]+}/branches")
    @ApiOperation(
            value = "Get branches",
            notes = "Returns a list of the repository branches associated with a given build name",
            response = String.class, responseContainer = "List")
    public List<String> getBranches(
            @ApiParam(value = "BUILD name", required = true)@PathParam("name") String buildName) throws Exception {
        return buildDAO.getBranches(buildName);
    }

    @GET
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get build info",
            notes = "Returns a build object given a build id",
            response = BuildBean.class)
    public BuildBean get(
            @ApiParam(value = "BUILD id", required = true)@PathParam("id") String id) throws Exception {
        BuildBean buildBean = buildDAO.getById(id);
        if (buildBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND, String.format("BUILD %s does not exist.", id));
        }
        return buildBean;
    }

    @GET
    @Path("/{id : [a-zA-Z0-9\\-_]+}/tags")
    @ApiOperation(
        value = "Get build info with its tags",
        notes = "Returns a build object given a build id",
        response = BuildTagBean.class)
    public BuildTagBean getWithTag(
        @ApiParam(value = "BUILD id", required = true)@PathParam("id") String id) throws Exception {
        BuildBean buildBean = buildDAO.getById(id);
        if (buildBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND, String.format("BUILD %s does not exist.", id));
        }

        BuildTagsManager manager = new BuildTagsManagerImpl(this.tagDAO);

        return new BuildTagBean(buildBean, manager.getEffectiveBuildTag(buildBean));
    }

    @GET
    public List<BuildBean> get(@QueryParam("commit") String scmCommit,
        @QueryParam("name") String buildName, @QueryParam("branch") String scmBranch,
        @QueryParam("pageIndex") Optional<Integer> pageIndex,
        @QueryParam("pageSize") Optional<Integer> pageSize, @QueryParam("before") Long before,
        @QueryParam("after") Long after) throws Exception {


        if (StringUtils.isEmpty(scmCommit) && StringUtils.isEmpty(buildName)) {
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST,
                "Require either commit id or build name in the request.");
        }

        return buildDAO.get(scmCommit, buildName, scmBranch, pageIndex, pageSize, before, after);
    }

    @GET
    @Path("/current")
    public List<BuildBean> getCurrentBuildsWithGroupName(@QueryParam("group") String groupName) throws Exception {
        if (StringUtils.isEmpty(groupName)) {
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST,
                "Require group name in the request.");
        }
        return buildDAO.getCurrentBuildsByGroupName(groupName);
    }

    @GET
    @Path("/tags")
    @ApiOperation(
        value = "Get build info along with the build tag info for a given build name",
        notes = "Return a bean object containing the build and the build tag",
        response = BuildTagBean.class
    )
    public List<BuildTagBean> getBuildsWithTags(@QueryParam("commit") String scmCommit,
        @QueryParam("name") String buildName, @QueryParam("branch") String scmBranch,
        @QueryParam("pageIndex") Optional<Integer> pageIndex,
        @QueryParam("pageSize") Optional<Integer> pageSize, @QueryParam("before") Long before,
        @QueryParam("after") Long after) throws Exception {

        if (StringUtils.isEmpty(buildName) && StringUtils.isEmpty(scmCommit)) {
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST,
                "Require either commit or build name in the request.");
        }

        List<BuildBean> builds =
            buildDAO.get(scmCommit, buildName, scmBranch, pageIndex, pageSize, before, after);

        BuildTagsManager manager = new BuildTagsManagerImpl(this.tagDAO);
        return manager.getEffectiveTagsWithBuilds(builds);
    }

    @POST
    @ApiOperation(
            value = "Publish a build",
            notes = "Publish a build given a build object",
            response = Response.class)
    public Response publish(
            @Context SecurityContext sc,
            @ApiParam(value = "BUILD object", required = true)@Valid BuildBean buildBean) throws Exception {
        if (StringUtils.isEmpty(buildBean.getScm())) {
            buildBean.setScm(sourceControlManager.getType());
        }

        if (StringUtils.isEmpty(buildBean.getScm_commit_7())) {
            buildBean.setScm_commit_7(StringUtils.substring(buildBean.getScm_commit(), 0, 7));

        }

        if (StringUtils.isEmpty(buildBean.getScm_info())) {
            buildBean.setScm_info(sourceControlManager.generateCommitLink(buildBean.getScm_repo(), buildBean.getScm_commit()));
        }

        if (StringUtils.isEmpty(buildBean.getPublish_info())) {
            buildBean.setPublish_info("UNKNOWN");
        }

        if (buildBean.getPublish_date() == null) {
            buildBean.setPublish_date(System.currentTimeMillis());
        }

        if (buildBean.getCommit_date() == null) {
            buildBean.setCommit_date(System.currentTimeMillis());
        }

        // Set who published the build
        buildBean.setPublisher(sc.getUserPrincipal().getName());

        // We append commit SHA after build id to make build directory name human friendly
        String id = CommonUtils.getBase64UUID();
        String buildId = String.format("%s_%s", id, buildBean.getScm_commit_7());
        buildBean.setBuild_id(buildId);
        
        buildDAO.insert(buildBean);
        LOG.info("Successfully published build {} by {}.", buildId, sc.getUserPrincipal().getName());

        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI buildUri = ub.path(buildId).build();
        buildBean = buildDAO.getById(buildId);
        return Response.created(buildUri).entity(buildBean).build();
    }


    @DELETE
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
        value = "Delete a build",
        notes = "Deletes a build given a build id")
    public void delete(
        @Context SecurityContext sc,
        @ApiParam(value = "BUILD id", required = true)@PathParam("id") String id) throws Exception {
        authorizer.authorize(sc, new Resource(Resource.ALL, Resource.Type.SYSTEM), Role.OPERATOR);
        BuildBean buildBean = buildDAO.getById(id);
        if (buildBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND, String.format("BUILD %s does not exist.", id));
        }
        buildDAO.delete(id);
        LOG.info("{} successfully deleted build {}", sc.getUserPrincipal().getName(), id);
    }
}
