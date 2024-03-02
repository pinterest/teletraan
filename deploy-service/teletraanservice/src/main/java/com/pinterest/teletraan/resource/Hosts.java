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
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.*;

import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import java.util.Collection;
import java.util.List;

@PermitAll
@Path("/v1/hosts")
@Api(tags = "Hosts and Systems")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Hosts and Systems", description = "Host info APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// TODO: CDP-7701 Add authorization to hosts endpoints
public class Hosts {
    private static final Logger LOG = LoggerFactory.getLogger(Hosts.class);
    private HostDAO hostDAO;
    private EnvironHandler environHandler;

    public Hosts(@Context TeletraanServiceContext context) {
        hostDAO = context.getHostDAO();
        environHandler = new EnvironHandler(context);
    }

    @POST
    public void addHost(@Context SecurityContext sc,
                        @Valid HostBean hostBean) throws Exception {
        String operator = sc.getUserPrincipal().getName();
        hostBean.setLast_update(System.currentTimeMillis());
        if (hostBean.getCreate_date() == null) {
            hostBean.setCreate_date(System.currentTimeMillis());
        }
        if (hostBean.getState() == null) {
            hostBean.setState(HostState.PROVISIONED);
        }
        hostDAO.insert(hostBean);
        LOG.info(String.format("Successfully added one host by %s: %s", operator, hostBean.toString()));
    }

    @PUT
    @Path("/{hostId : [a-zA-Z0-9\\-_]+}")
    public void updateHost(@Context SecurityContext sc,
                           @PathParam("hostId") String hostId,
                           @Valid HostBean hostBean) throws Exception {
        String operator = sc.getUserPrincipal().getName();
        hostBean.setHost_id(hostId);
        hostBean.setLast_update(System.currentTimeMillis());
        hostDAO.updateHostById(hostId, hostBean);
        LOG.info(String.format("Successfully updated one host by %s: %s", operator, hostBean.toString()));
    }

    @DELETE
    @Path("/{hostId : [a-zA-Z0-9\\-_]+}")
    public void stopHost(@Context SecurityContext sc,
            @PathParam("hostId") String hostId,
            @ApiParam(value = "Replace the host or not") @QueryParam("replaceHost") Optional<Boolean> replaceHost)
            throws Exception {
        String operator = sc.getUserPrincipal().getName();
        environHandler.stopServiceOnHost(hostId, replaceHost.or(true));
        LOG.info(String.format("Successfully stopped host %s by %s", hostId, operator));
    }

    @GET
    @Path("/{hostName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get host info objects by host name",
            notes = "Returns a list of host info objects given a host name",
            response = HostBean.class, responseContainer = "List")
    public List<HostBean> get(
            @ApiParam(value = "Host name", required = true)@PathParam("hostName") String hostName) throws Exception {
        return hostDAO.getHosts(hostName);
    }

    @GET
    @Path("/id/{hostId : [a-zA-Z0-9\\-_]+}")
    public Collection<HostBean> getById(@PathParam("hostId") String hostId) throws Exception {
        return hostDAO.getHostsByHostId(hostId);
    }
}
