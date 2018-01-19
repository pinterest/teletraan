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

import com.pinterest.deployservice.bean.AlarmBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.handler.ConfigHistoryHandler;
import com.pinterest.deployservice.handler.EnvironHandler;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/alarms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvAlarms {
    private static final Logger LOG = LoggerFactory.getLogger(EnvWebHooks.class);
    private EnvironHandler environHandler;
    private EnvironDAO environDAO;
    private ConfigHistoryHandler configHistoryHandler;
    private Authorizer authorizer;

    @Context
    UriInfo uriInfo;

    public EnvAlarms(TeletraanServiceContext context) {
        environHandler = new EnvironHandler(context);
        configHistoryHandler = new ConfigHistoryHandler(context);
        environDAO = context.getEnvironDAO();
        authorizer = context.getAuthorizer();
    }

    @GET
    public List<AlarmBean> get(@PathParam("envName") String envName,
        @PathParam("stageName") String stageName) throws Exception {
        EnvironBean environBean = Utils.getEnvStage(environDAO, envName, stageName);
        return environHandler.getAlarms(environBean);
    }

    @PUT
    public void update(@PathParam("envName") String envName, @PathParam("stageName") String stageName,
        @Valid List<AlarmBean> alarmBeans, @Context SecurityContext sc) throws Exception {
        EnvironBean environBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(environBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String userName = sc.getUserPrincipal().getName();
        environHandler.updateAlarms(environBean, alarmBeans, userName);
        configHistoryHandler.updateConfigHistory(environBean.getEnv_id(), Constants.TYPE_ENV_ALARM, alarmBeans, userName);
        configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, environBean.getEnv_id(), Constants.TYPE_ENV_ALARM, userName);
        LOG.info("Successfully updated alarms {} for env {}/{} by {}.",
            alarmBeans, envName, stageName, userName);
    }
}
