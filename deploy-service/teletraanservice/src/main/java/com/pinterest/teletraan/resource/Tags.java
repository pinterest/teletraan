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

import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.handler.BuildTagHandler;
import com.pinterest.deployservice.handler.TagHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RolesAllowed(TeletraanPrincipalRole.Names.READ)
@Api(tags = "Tags")
@Path("/v1/tags")
@Produces(MediaType.APPLICATION_JSON)
@SwaggerDefinition(
        tags = {
            @Tag(name = "Tags", description = "Tagging APIs"),
        })
@Consumes(MediaType.APPLICATION_JSON)
public class Tags {
    private static final Logger LOG = LoggerFactory.getLogger(Tags.class);
    private final TagDAO tagDAO;
    private final HashMap<TagTargetType, TagHandler> handlers = new HashMap<>();

    public Tags(@Context TeletraanServiceContext context) {
        this.tagDAO = context.getTagDAO();
        this.handlers.put(TagTargetType.BUILD, new BuildTagHandler(context));
    }

    @GET
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get tags with a given id",
            notes = "Return a TagBean objects",
            response = TagBean.class)
    public TagBean getById(@PathParam("id") String id) throws Exception {
        TagBean ret = tagDAO.getById(id);
        if (ret == null) {
            throw new WebApplicationException(
                    String.format("Tag %s does not exist.", id), Response.Status.NOT_FOUND);
        }
        return ret;
    }

    @GET
    @Path("/targets/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get tags applied on a target id",
            notes = "Return a list of TagBean objects",
            response = List.class)
    public List<TagBean> getByTargetId(@PathParam("id") String targetId) throws Exception {

        if (StringUtils.isEmpty(targetId)) {
            throw new WebApplicationException(
                    "Require at least one of targetId, targetType, value specified in the request.",
                    Response.Status.BAD_REQUEST);
        }

        return tagDAO.getByTargetId(targetId);
    }

    @GET
    @Path("/targets/{id : [a-zA-Z0-9\\-_]+}/latest")
    @ApiOperation(
            value = "Get tags applied on a target id",
            notes = "Return a list of TagBean objects",
            response = List.class)
    public TagBean getLatestByTargetId(@PathParam("id") String targetId) throws Exception {
        if (StringUtils.isEmpty(targetId)) {
            throw new WebApplicationException(
                    "Require at least one of targetId, targetType, value specified in the request.",
                    Response.Status.BAD_REQUEST);
        }
        return tagDAO.getLatestByTargetId(targetId);
    }

    @GET
    @Path("/values/{value : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get tags with the given value",
            notes = "Return a list of TagBean object with given value",
            response = List.class)
    public List<TagBean> getByValue(@PathParam("value") String value) throws Exception {

        if (StringUtils.isEmpty(value)) {
            throw new WebApplicationException(
                    "Require at least one of targetId, targetType, value specified in the request.",
                    Response.Status.BAD_REQUEST);
        }

        try {
            return tagDAO.getByValue(TagValue.valueOf(value.toUpperCase()));

        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(
                    String.format("%s is not a valid tag Value.", value),
                    Response.Status.BAD_REQUEST);
        }
    }

    @POST
    @ApiOperation(
            value = "Create a tag",
            notes = "Create a tag on an object",
            response = Response.class)
    public Response create(
            @Context SecurityContext sc,
            @Context UriInfo uriInfo,
            @ApiParam(value = "Tag object", required = true) @Valid TagBean tag)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();
        TagBean retEntity = new TagBean();
        if (handlers.containsKey(tag.getTarget_type())) {
            LOG.debug("Found handler for target type {}", tag.getTarget_type());
            retEntity = handlers.get(tag.getTarget_type()).createTag(tag, operator);
        } else {
            LOG.debug("No handler found for target type {}. Use default", tag.getTarget_type());
            // Default logic. Ideally each type should write its own handler.
            // At current time, ignore the target id object existence test. The
            // handler should do that.
            tag.setId(CommonUtils.getBase64UUID());
            tag.setOperator(operator);
            tag.setCreated_date(System.currentTimeMillis());
            tagDAO.insert(tag);
            LOG.info("{} successfully created tag {}", operator, tag.getId());

            retEntity = tagDAO.getById(tag.getId());
        }
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI buildUri = ub.path(retEntity.getId()).build();
        return Response.created(buildUri).entity(retEntity).build();
    }

    @DELETE
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(value = "Delete a tag", notes = "Deletes a build given a tag id")
    public void delete(
            @Context SecurityContext sc,
            @ApiParam(value = "tag id", required = true) @PathParam("id") String id)
            throws Exception {
        TagBean tagBean = tagDAO.getById(id);
        if (tagBean == null) {
            throw new WebApplicationException(
                    String.format("Tag %s does not exist.", id), Response.Status.NOT_FOUND);
        }
        tagDAO.delete(id);
        LOG.info("{} successfully deleted tag {}", sc.getUserPrincipal().getName(), id);
    }
}
