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


import com.pinterest.arcee.bean.HealthCheckBean;
import com.pinterest.arcee.bean.HealthCheckType;
import com.pinterest.arcee.bean.ImageBean;
import com.pinterest.arcee.dao.ImageDAO;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.arcee.handler.HealthCheckHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;

import com.google.common.base.Optional;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/v1/machine_images")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Images {
    private static final Logger LOG = LoggerFactory.getLogger(Images.class);
    private static final Integer DEFAULT_PAGE_SIZE = 25;
    private ImageDAO imageDAO;
    private final Authorizer authorizer;
    private HealthCheckHandler healthCheckHandler;

    public Images(TeletraanServiceContext context) {
        imageDAO = context.getImageDAO();
        authorizer = context.getAuthorizer();
        healthCheckHandler = new HealthCheckHandler(context);
    }

    @POST
    public void publish(@Context SecurityContext sc, @Valid ImageBean amiBean) throws Exception {
        authorizer.authorize(sc, new Resource(Resource.ALL, Resource.Type.SYSTEM), Role.OPERATOR);
        if (amiBean.getPublish_date() == null) {
            amiBean.setPublish_date(System.currentTimeMillis());
        }

        amiBean.setQualified(false);
        imageDAO.insertOrUpdate(amiBean);
        LOG.info("Publish new ami {} for app {}", amiBean.getId(), amiBean.getApp_name());

        HealthCheckBean healthCheckBean = new HealthCheckBean();
        healthCheckBean.setType(HealthCheckType.AMI_TRIGGERED);
        healthCheckBean.setAmi_id(amiBean.getId());
        List<String> healthCheckIds = healthCheckHandler.createHealthCheck(healthCheckBean);
        LOG.info("Add new health checks, ids: {}", Joiner.on(",").join(healthCheckIds));
    }

    @GET
    @Path("/appnames")
    public List<String> getApplicationNames() throws Exception {
        return imageDAO.getAppNames();
    }

    @GET
    public List<ImageBean> getImagesByAppname(@QueryParam("app") Optional<String> appName,
        @QueryParam("start") Optional<Integer> start, @QueryParam("size") Optional<Integer> size) throws Exception {
        return imageDAO.getImages(appName.orNull(), start.or(1), size.or(DEFAULT_PAGE_SIZE));
    }

    @GET
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    public ImageBean getImageById(@PathParam("id") String id) throws Exception {
        if (StringUtils.isEmpty(id)) {
            throw new TeletaanInternalException(Response.Status.BAD_REQUEST, "Require image id for the request.");
        }

        return imageDAO.getById(id);
    }
}
