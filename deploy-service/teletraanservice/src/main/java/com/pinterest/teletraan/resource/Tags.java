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

import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.handler.BuildTagHandler;
import com.pinterest.deployservice.handler.TagHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

@Path("/v1/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Tags {
    private static final Logger LOG = LoggerFactory.getLogger(Tags.class);
    private final TagDAO tagDAO;
    private final BuildDAO buildDAO;
    private final EnvironDAO environDAO;
    private final HashMap<TagTargetType, TagHandler> handlers = new HashMap<>();

    public Tags(TeletraanServiceContext context)
    {
        this.tagDAO = context.getTagDAO();
        this.buildDAO = context.getBuildDAO();
        this.environDAO = context.getEnvironDAO();
        this.handlers.put(TagTargetType.BUILD, new BuildTagHandler(context));
    }

    @Context
    UriInfo uriInfo;

    @GET
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
        value = "Get tags with a given id",
        notes = "Return a TagBean objects",
        response = TagBean.class)
    public TagBean getById(@PathParam("id") String id)
        throws Exception {
        TagBean ret = tagDAO.getById(id);
        if (ret == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND,
                String.format("Tag %s does not exist.", id));
        }
        return ret;
    }

    @GET
    @Path("/targets/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
        value = "Get tags applied on a target id",
        notes = "Return a list of TagBean objects",
        response = List.class)
    public List<TagBean> getByTargetId(@PathParam("targetId") String targetId) throws Exception {

        if (StringUtils.isEmpty(targetId)) {
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST,
                "Require at least one of targetId, targetType, value specified in the request.");
        }

        return tagDAO.getByTargetId(targetId);
    }


    @GET
    @Path("/values/{value : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
        value = "Get tags with the given value",
        notes = "Return a list of TagBean object with given value",
        response = List.class)
    public List<TagBean> getByValue(@PathParam("value") String value)
        throws Exception {

        if (StringUtils.isEmpty(value)) {
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST,
                "Require at least one of targetId, targetType, value specified in the request.");
        }

        try {
            return tagDAO.getByValue(TagValue.valueOf(value.toUpperCase()));

        } catch (IllegalArgumentException e) {
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST,
                String.format("%s is not a valid tag Value.", value));
        }
    }


    @POST
    @ApiOperation(
        value = "Create a tag",
        notes = "Create a tag on an object",
        response = Response.class)
    public Response create(@Context SecurityContext sc,
        @ApiParam(value = "Tag object", required = true) @Valid TagBean tag) throws Exception {

        TagBean retEntity = new TagBean();
        if (handlers.containsKey(tag.getTarget_type())) {
            LOG.debug("Found handler for target type {}", tag.getTarget_type().toString());
            retEntity = handlers.get(tag.getTarget_type()).createTag(tag, sc);
        } else {
            LOG.debug("No handler found for target type {}. Use default",
                tag.getTarget_type().toString());
            //Default logic. Ideally each type should write its own handler.
            //At current time, ignore the target id object existence test. The
            //handler should do that.
            tag.setId(CommonUtils.getBase64UUID());
            tag.setOperator(sc.getUserPrincipal().getName());
            tag.setCreated_date(System.currentTimeMillis());
            tagDAO.insert(tag);
            LOG.info("{} successfully created tag {}", sc.getUserPrincipal().getName(),
                tag.getId());

            retEntity = tagDAO.getById(tag.getId());
        }
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI buildUri = ub.path(retEntity.getId()).build();
        return Response.created(buildUri).entity(retEntity).build();
    }

    @DELETE
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
        value = "Delete a tag",
        notes = "Deletes a build given a tag id")
    public void delete(@Context SecurityContext sc,
        @ApiParam(value = "tag id", required = true) @PathParam("id") String id) throws Exception {
        TagBean tagBean = tagDAO.getById(id);
        if (tagBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND,
                String.format("Tag %s does not exist.", id));
        }
        tagDAO.delete(id);
        LOG.info("{} successfully deleted tag {}", sc.getUserPrincipal().getName(), id);
    }
}
