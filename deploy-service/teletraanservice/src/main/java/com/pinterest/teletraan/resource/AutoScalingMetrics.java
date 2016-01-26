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


import com.pinterest.arcee.bean.MetricDatumBean;
import com.pinterest.arcee.handler.MetricHandler;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

// TODO (jihe) refactor this endpoint to /v1/autoscaling/metrics
@Path("/v1/metrics/autoscaling")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AutoScalingMetrics {
    private MetricHandler metricHandler;
    private EnvironDAO environDAO;
    private enum LatencyType {
        LAUNCH,
        DEPLOY
    }

    public AutoScalingMetrics(ServiceContext context) {
        metricHandler = new MetricHandler(context);
        environDAO = context.getEnvironDAO();
    }

    @GET
    @Path("/groups/size")
    public Collection<MetricDatumBean> getGroupSizeMetrics(@QueryParam("groupName") String groupName,
                                                     @QueryParam("start") String startFrom) throws Exception {
        return metricHandler.getGroupSizeMetrics(groupName, startFrom);
    }

    @GET
    @Path("/latency/")
    public Collection<MetricDatumBean> getLatencyMetrics(@QueryParam("envId") String envId, @QueryParam("type") String type,
                                                   @QueryParam("start") String startFrom) throws Exception {
        LatencyType latencyType = LatencyType.valueOf(LatencyType.class, type.toUpperCase());
        EnvironBean environBean = environDAO.getById(envId);
        if (latencyType == LatencyType.LAUNCH) {
            return metricHandler.getLaunchLatencyMetrics(environBean.getEnv_name(), environBean.getStage_name(), startFrom);
        } else if (latencyType == LatencyType.DEPLOY) {
            return metricHandler.getDeployLatencyMetrics(environBean.getEnv_name(), environBean.getStage_name(), startFrom);
        }

        throw new TeletaanInternalException(Response.Status.BAD_REQUEST, String.format("Unknow action type:%s", type));
    }

    @GET
    @Path("/groups/")
    public Collection<MetricDatumBean> getMetricData(@QueryParam("groupName") String groupName,
                                               @QueryParam("metricName") String metricName,
                                               @QueryParam("start") String startFrom) throws Exception {
        return metricHandler.getMetricData(groupName, metricName, startFrom);
    }

    @GET
    @Path("/raw_metrics/")
    public Collection<MetricDatumBean> getRawMetricData(@QueryParam("metricName") String metricName,
                                                  @QueryParam("start") String startFrom) throws Exception {
        return metricHandler.getRawMetricData(metricName, startFrom);
    }
}
