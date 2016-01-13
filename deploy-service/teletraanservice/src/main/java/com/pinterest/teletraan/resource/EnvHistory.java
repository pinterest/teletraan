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

import com.google.common.base.Optional;
import com.pinterest.deployservice.bean.ConfigHistoryBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.ConfigHistoryDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.teletraan.TeletraanServiceContext;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvHistory {
    private static final int DEFAULT_SIZE = 30;
    private final EnvironDAO environDAO;
    private final ConfigHistoryDAO configHistoryDAO;

    public EnvHistory(TeletraanServiceContext context) {
        environDAO = context.getEnvironDAO();
        configHistoryDAO = context.getConfigHistoryDAO();
    }

    @GET
    public List<ConfigHistoryBean> get(@PathParam("envName") String envName,
        @PathParam("stageName") String stageName,
        @QueryParam("pageIndex") Optional<Integer> pageIndex,
        @QueryParam("pageSize") Optional<Integer> pageSize) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        return configHistoryDAO.getByConfigId(envBean.getEnv_id(),
            pageIndex.or(1), pageSize.or(DEFAULT_SIZE));
    }
}
