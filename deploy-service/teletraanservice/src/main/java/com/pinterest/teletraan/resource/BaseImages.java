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

import com.pinterest.clusterservice.bean.BaseImageBean;
import com.pinterest.clusterservice.bean.CloudProvider;
import com.pinterest.clusterservice.dao.BaseImageDAO;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.teletraan.TeletraanServiceContext;

import com.google.common.base.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/v1/base_images")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BaseImages {
    private static final Logger LOG = LoggerFactory.getLogger(BaseImages.class);
    private final static int DEFAULT_INDEX = 1;
    private final static int DEFAULT_SIZE = 30;
    private BaseImageDAO baseImageDAO;

    public BaseImages(TeletraanServiceContext context) {
        baseImageDAO = context.getBaseImageDAO();
    }

    @POST
    public void create(@Context SecurityContext sc, @Valid BaseImageBean bean) throws Exception {
        bean.setId(CommonUtils.getBase64UUID());
        LOG.info(String.format("Create new base image %s", bean.toString()));
        bean.setQualified(false);
        if (bean.getPublish_date() == null) {
            bean.setPublish_date(System.currentTimeMillis());
        }
        baseImageDAO.insert(bean);
        // TODO insert health check and update clusters
    }

    @GET
    public Collection<BaseImageBean> getAll(@QueryParam("pageIndex") Optional<Integer> pageIndex,
                                            @QueryParam("pageSize") Optional<Integer> pageSize) throws Exception {
        return baseImageDAO.getAll(pageIndex.or(DEFAULT_INDEX), pageSize.or(DEFAULT_SIZE));
    }

    @GET
    @Path("/names")
    public Collection<String> getAbstractNames(@QueryParam("provider") String provider) throws Exception {
        return baseImageDAO.getAbstractNamesByProvider(provider);
    }

    @GET
    @Path("/names/{name : [a-zA-Z0-9\\-_]+}")
    public Collection<BaseImageBean> getByAbstractName(@PathParam("name") String name) throws Exception {
        return baseImageDAO.getByAbstractName(name);
    }

    @GET
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    public BaseImageBean getById(@PathParam("id") String id) throws Exception {
        return baseImageDAO.getById(id);
    }

    @GET
    @Path("/provider")
    public List<CloudProvider> getCloudProviders() throws Exception {
        return new ArrayList<CloudProvider>(Arrays.asList(CloudProvider.values()));
    }
}
