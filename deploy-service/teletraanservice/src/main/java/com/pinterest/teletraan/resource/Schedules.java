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

import com.pinterest.arcee.handler.ProvisionHandler;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.ScheduleBean;
import com.pinterest.deployservice.dao.ScheduleDAO;
import com.pinterest.deployservice.dao.EnvironDAO;

import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.deployservice.common.CommonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import java.util.Collection;
import java.util.List;

@Path("/v1/schedules")
// @Api(tags = "Hosts and Systems")
// @SwaggerDefinition(
//         tags = {
//                 @Tag(name = "Hosts and Systems", description = "Host info APIs"),
//         }
// )
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Schedules {
    private static final Logger LOG = LoggerFactory.getLogger(Schedules.class);
    private ScheduleDAO scheduleDAO;
    private EnvironDAO environDAO;

    private ProvisionHandler provisionHandler;

    public Schedules(TeletraanServiceContext context) {
        scheduleDAO = context.getScheduleDAO();
        environDAO = context.getEnvironDAO();
        provisionHandler = new ProvisionHandler(context);
    }

    @GET
    @Path("/{scheduleId : [a-zA-Z0-9\\-_]+}")
    public ScheduleBean getSchedule(
            @Context SecurityContext sc,
            @PathParam("scheduleId") String scheduleId) throws Exception {

        String operator = sc.getUserPrincipal().getName();

        ScheduleBean scheduleBean = scheduleDAO.getById(scheduleId);

        // if (scheduleBean == null) {
        //     throw new TeletaanInternalException(Response.Status.NOT_FOUND,
        //         String.format("Schedule %s does not exist.", scheduleId));
        // }
        if (scheduleBean!=null) {
            LOG.info(scheduleBean.toString());

        }
        return scheduleBean;
        // LOG.info(String.format("Successfully added one host by %s: %s", operator, hostBean.toString()));
    }
    
    @POST
    @Path("/update/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}")
    public void updateSchedule(
            @Context SecurityContext sc,
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @Valid ScheduleBean bean) throws Exception {
        String operator = sc.getUserPrincipal().getName();
        // hostBean.setHost_id(hostId);
        // hostBean.setLast_update(System.currentTimeMillis());
        EnvironBean envBean = environDAO.getByStage(envName, stageName);
        String scheduleId = envBean.getSchedule_id();
        String cooldownTimes = bean.getCooldown_times();
        String hostNumbers = bean.getHost_numbers();
        Integer totalSessions = bean.getTotal_sessions();
        LOG.info(String.format("why is it not coming here"));
        LOG.info(String.format("Total Sessions:" + Integer.toString(totalSessions)));
        if (totalSessions > 0) {          
            ScheduleBean scheduleBean = new ScheduleBean();
            scheduleBean.setState_start_time(System.currentTimeMillis());
            scheduleBean.setCooldown_times(cooldownTimes);
            scheduleBean.setHost_numbers(hostNumbers);
            scheduleBean.setTotal_sessions(totalSessions);
            LOG.info(scheduleBean.toString());

            if (scheduleId == null) {
                scheduleId = CommonUtils.getBase64UUID();
                envBean.setSchedule_id(scheduleId);
                environDAO.update(envName, stageName, envBean);
                scheduleBean.setId(scheduleId);
                scheduleDAO.insert(scheduleBean);
                LOG.info(String.format("Successfully inserted one env %s (%s)'s schedule by %s: %s", envName, stageName, operator, scheduleBean.toString()));
            } else {
                scheduleBean.setId(scheduleId);
                scheduleDAO.update(scheduleBean, scheduleId);
                LOG.info(String.format("Successfully updated one env %s (%s)'s schedule by %s: %s", envName, stageName, operator, scheduleBean.toString()));
            }
        } else if (scheduleId != null) { // no more sessions --> delete schedule
            LOG.info(String.format("In here!!!"));
            scheduleDAO.delete(scheduleId); 
            envBean.setSchedule_id(null);
            environDAO.update(envName, stageName, envBean);
        }
    }

    @DELETE
    @Path("/delete/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}")
    public void deleteSchedule(
            @Context SecurityContext sc,
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName) throws Exception {
        String operator = sc.getUserPrincipal().getName();
        EnvironBean envBean = environDAO.getByStage(envName, stageName);
        String scheduleId = envBean.getSchedule_id();
        scheduleDAO.delete(scheduleId); // make sure it deletes it from the enviroment thing too 
        envBean.setSchedule_id(null);
        environDAO.update(envName, stageName, envBean);
        // LOG.info(String.format("Successfully stopped host %s by %s", hostId, operator));
        //Make better log info 
    }

    // @GET
    // @Path("/{scheduleId : [a-zA-Z0-9\\-_]+}")
    // @ApiOperation(
    //         value = "Get host info objects by host name",
    //         notes = "Returns a list of host info objects given a host name",
    //         response = HostBean.class, responseContainer = "List")
    // public List<HostBean> get(
    //         @ApiParam(value = "Host name", required = true)@PathParam("hostName") String hostName) throws Exception {
    //     return hostDAO.getHosts(hostName);
    // }
}
