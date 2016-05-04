/*
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
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/v1/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Tags {
    private static final Logger LOG = LoggerFactory.getLogger(Tags.class);
    private final TagDAO tagDAO;
    private final BuildDAO buildDAO;
    private final EnvironDAO environDAO;

    @Context
    UriInfo uriInfo;

    public Tags(TeletraanServiceContext context) {
        this.tagDAO = context.getTagDAO();
        this.buildDAO = context.getBuildDAO();
        this.environDAO = context.getEnvironDAO();
    }


    @GET
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    public TagBean getById(@PathParam("id") String id) throws Exception {
        TagBean ret = tagDAO.getById(id);
        if (ret == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND,
                    String.format("TagInfo %s does not exist.", id));
        }
        return ret;
    }

    @GET
    @Path("/builds")
    public List<BuildTagBean>  getBuildsWithTags(@QueryParam("commit") String scmCommit,
                                                     @QueryParam("name") String buildName,
                                                     @QueryParam("branch") String scmBranch,
                                                     @QueryParam("pageIndex") Optional<Integer> pageIndex,
                                                     @QueryParam("pageSize") Optional<Integer> pageSize,
                                                     @QueryParam("before") Long before,
                                                     @QueryParam("after") Long after) throws Exception{

        if(StringUtils.isEmpty(scmCommit) && StringUtils.isEmpty(buildName)) {
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST,
                    "Require either commit id or build name in the request.");
        }

        List<BuildBean> builds = buildDAO.get(scmCommit,buildName,scmBranch,pageIndex,pageSize,before,after);

        BuildTagsManager manager = new BuildTagsManagerImpl(this.tagDAO, this.environDAO);
        return manager.getEffectiveTagsWithBuilds(builds);
    }

    @POST
    @Path(("/builds"))
    @ApiOperation(
            value = "Tagging a build",
            notes = "Tag a build",
            response = Response.class)
    public Response createBuildTag(@Context SecurityContext sc,
                               @Valid TagBean bean) throws Exception {
        BuildBean build = this.buildDAO.getById(bean.getTarget_id());
        if(build != null) {

            List<TagBean> existingTags = tagDAO.getByTargetId(build.getBuild_id());

            if (valueAlreadyExists(existingTags, bean.getTarget_id(), bean.getValue())){
                throw new TeletaanInternalException(Response.Status.CONFLICT, "Same tag has already bean applied.");
            }

            if(bean.getValue()==TagValue.BadBuild &&
                    valueAlreadyExists(existingTags,bean.getTarget_id(),TagValue.GoodBuild)){
                throw new TeletaanInternalException(Response.Status.CONFLICT,
                        "GoodBuild Tag exists. Must delete it first to add a BadBuild tag");
            }

            if(bean.getValue()==TagValue.GoodBuild &&
                    valueAlreadyExists(existingTags,bean.getTarget_id(),TagValue.BadBuild)){
                throw new TeletaanInternalException(Response.Status.CONFLICT,
                        "BadBuild Tag exists. Must delete it first to add a GoodBuild tag");
            }

            bean.setId(CommonUtils.getBase64UUID());
            bean.setTarget_name(build.getBuild_name());
            bean.setTarget_type(TagTargetType.Build);
            bean.serializeTagMetaInfo(build);
            bean.setOperator(sc.getUserPrincipal().getName());
            bean.setCreated_date(System.currentTimeMillis());

            tagDAO.insert(bean);
            LOG.info("Successfully tagged {} on build {} by {}. Tag id is {}",
                    bean.getValue(), bean.getTarget_id(), bean.getOperator(), bean.getId());

            TagBean tag = tagDAO.getById(bean.getId());
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            URI buildUri = ub.path(tag.getId()).build();
             return Response.created(buildUri).entity(tag).build();
        }
        else{
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST,
                    "Build cannot be found.");
        }
    }

    private static boolean valueAlreadyExists(List<TagBean> currentTags, String targetId, TagValue value){
        boolean ret = false;
        for(TagBean tag:currentTags){
            if (tag.getTarget_id().equals(targetId) && tag.getValue() == value){
                ret = true;
                break;
            }
        }
        return ret;
    }
}
