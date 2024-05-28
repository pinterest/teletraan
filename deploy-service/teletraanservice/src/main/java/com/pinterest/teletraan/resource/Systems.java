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


import com.pinterest.deployservice.bean.ChatMessageBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.chat.ChatManager;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.QueryParam;
import com.google.common.base.Optional;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@PermitAll
@Path("/v1/system")
@Api(tags = "Hosts and Systems")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Systems {

    private static final Logger LOG = LoggerFactory.getLogger(Systems.class);
    private SourceControlManagerProxy sourceControlManagerProxy;
    private HostDAO hostDAO;
    private ChatManager chatManager;

    public Systems(@Context TeletraanServiceContext context) {
        sourceControlManagerProxy = context.getSourceControlManagerProxy();
        chatManager = context.getChatManager();
        hostDAO = context.getHostDAO();
    }

    @GET
    @Path("/scm_link_template")
    @ApiOperation(
        value = "Get SCM commit link template",
        notes = "Returns a Source Control Manager specific commit link template.",
        response = String.class)
    public String getSCMLinkTemplate(@QueryParam("scm") Optional<String> scm) throws Exception {
        return String
            .format("{\"template\": \"%s\"}", sourceControlManagerProxy.getCommitLinkTemplate(scm.or("")));
    }

    @GET
    @Path("/scm_url")
    @ApiOperation(
        value = "Get SCM url",
        notes = "Returns a Source Control Manager Url.",
        response = String.class)
    public String getSCMUrl(@QueryParam("scm") Optional<String> scm) throws Exception {
        return String.format("{\"url\": \"%s\"}", sourceControlManagerProxy.getUrlPrefix(scm.or("")));
    }

    @GET
    @Path("/get_host/{hostName : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
        value = "Get all host info",
        notes = "Returns a list of host info objects given a host name",
        response = HostBean.class, responseContainer = "List")
    public List<HostBean> getHosts(
        @ApiParam(value = "Host name", required = true) @PathParam("hostName") String hostName)
        throws Exception {
        return hostDAO.getHosts(hostName);
    }

    @POST
    @Path("/send_chat_message")
    @RolesAllowed(TeletraanPrincipalRole.Names.EXECUTE)
    @ResourceAuthZInfo(type = AuthZResource.Type.SYSTEM)
    @ApiOperation(
        value = "Send chat message",
        notes = "Sends a chatroom message given a ChatMessageRequest to configured chat client")
    public void sendChatMessage(
        @ApiParam(value = "ChatMessageRequest object",
            required = true) @Valid ChatMessageBean request)
        throws Exception {
        List<String> chatrooms = Arrays.asList(request.getTo().split(","));
        for (String chatroom : chatrooms) {
            chatManager.send(request.getFrom(), chatroom.trim(), request.getMessage(), "yellow");
        }
        LOG.info("Successfully handled send message requset {}", request);
    }
}
