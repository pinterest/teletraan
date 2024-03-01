/*sche
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

import com.pinterest.deployservice.bean.ScheduleState;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.ScheduleBean;
import com.pinterest.deployservice.dao.ScheduleDAO;
import com.pinterest.deployservice.dao.EnvironDAO;

import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.deployservice.common.CommonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/v1/schedules")

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Schedules {
    private static final Logger LOG = LoggerFactory.getLogger(Schedules.class);
    private ScheduleDAO scheduleDAO;
    private EnvironDAO environDAO;


    public Schedules(@Context TeletraanServiceContext context) {
        scheduleDAO = context.getScheduleDAO();
        environDAO = context.getEnvironDAO();
    }

    @GET
    @Path("/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/{scheduleId : [a-zA-Z0-9\\-_]+}")
    public ScheduleBean getSchedule(
            @Context SecurityContext sc,
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @PathParam("scheduleId") String scheduleId) throws Exception {

        String operator = sc.getUserPrincipal().getName();

        ScheduleBean scheduleBean = scheduleDAO.getById(scheduleId);
        if (scheduleBean!=null) {
            LOG.info(scheduleBean.toString());
        }
        return scheduleBean;
    }
    
    @PUT
    @Path("/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/schedules")
    public void updateSchedule(
            @Context SecurityContext sc,
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @Valid ScheduleBean bean) throws Exception {
        String operator = sc.getUserPrincipal().getName();
        EnvironBean envBean = environDAO.getByStage(envName, stageName);
        String scheduleId = envBean.getSchedule_id();
        String cooldownTimes = bean.getCooldown_times();
        String hostNumbers = bean.getHost_numbers();
        Integer totalSessions = bean.getTotal_sessions();
        if (totalSessions > 0) { // there is a schedule  
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
        } else if (scheduleId != null) { //there are no sessions, so delete the schedule
            scheduleDAO.delete(scheduleId); 
            environDAO.deleteSchedule(envName, stageName);
            LOG.info(String.format("Successfully deleted env %s (%s)'s schedule by %s", envName, stageName, operator));
        }
    }

    @PUT
    @Path("/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/override")
    public void overrideSession(
            @Context SecurityContext sc,
            @PathParam("envName") String envName,
            @PathParam("stageName") String stageName,
            @QueryParam("sessionNumber") Integer sessionNumber) throws Exception {
        String operator = sc.getUserPrincipal().getName();
        EnvironBean envBean = environDAO.getByStage(envName, stageName);
        String scheduleId = envBean.getSchedule_id();
        if (scheduleId == null) {
            LOG.info(String.format("Cannot override session, env %s has no schedule set", envName));
            return;
        }
        ScheduleBean scheduleBean = scheduleDAO.getById(scheduleId);
        Integer currentSession = scheduleBean.getCurrent_session();
        Integer totalSessions = scheduleBean.getTotal_sessions();
        if (sessionNumber != currentSession) {
            LOG.info(String.format("Overriding session %d is now invalid as deploy is already on session %d", sessionNumber, currentSession)); 
            return;   
        }
        if (sessionNumber == totalSessions) {
            scheduleBean.setState(ScheduleState.FINAL);
            LOG.info(String.format("Overrided session %d and currently working on the final deploy session", sessionNumber));    
        } else {
            scheduleBean.setCurrent_session(sessionNumber+1);
            scheduleBean.setState(ScheduleState.RUNNING);
            LOG.info(String.format("Overrided session %d and currently working on session %d", sessionNumber, currentSession+1));    
        }    
        scheduleBean.setState_start_time(System.currentTimeMillis());
        scheduleDAO.update(scheduleBean, scheduleId);
    }
}
