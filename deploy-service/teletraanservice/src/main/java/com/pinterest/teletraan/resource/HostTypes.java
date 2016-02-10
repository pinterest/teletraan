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


import com.pinterest.clusterservice.bean.HostTypeBean;
import com.pinterest.clusterservice.dao.HostTypeDAO;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.teletraan.TeletraanServiceContext;

import com.google.common.base.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/v1/host_types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HostTypes {
    private static final Logger LOG = LoggerFactory.getLogger(HostTypes.class);
    private final static int DEFAULT_INDEX = 1;
    private final static int DEFAULT_SIZE = 30;
    private final HostTypeDAO hostTypeDAO;

    public HostTypes(TeletraanServiceContext context) {
        hostTypeDAO = context.getHostTypeDAO();
    }

    @POST
    public void create(@Context SecurityContext sc, @Valid HostTypeBean bean) throws Exception {
        bean.setId(CommonUtils.getBase64UUID());
        LOG.info(String.format("Create new host type %s", bean.toString()));
        hostTypeDAO.insert(bean);
        // TODO udpate clusters, deprecated old host type
    }

    @GET
    public Collection<HostTypeBean> getAll(@QueryParam("pageIndex") Optional<Integer> pageIndex,
                                           @QueryParam("pageSize") Optional<Integer> pageSize) throws Exception {
        return hostTypeDAO.getAll(pageIndex.or(DEFAULT_INDEX), pageSize.or(DEFAULT_SIZE));
    }

    @GET
    @Path("/basic")
    public Collection<HostTypeBean> getByProviderAndBasic(@QueryParam("provider") String provider,
                                                          @QueryParam("basic") Optional<Boolean> basic) throws Exception {
        return hostTypeDAO.getByProviderAndBasic(provider, basic.or(true));
    }
}
