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
package com.pinterest.teletraan.health;

import com.codahale.metrics.health.HealthCheck;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployFilterBean;
import com.pinterest.deployservice.bean.DeployQueryResultBean;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.db.DeployQueryFilter;
import com.pinterest.teletraan.TeletraanServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GenericHealthCheck extends HealthCheck {
    private static final Logger LOG = LoggerFactory.getLogger(GenericHealthCheck.class);
    private final static int DEFAULT_SIZE = 2;
    private DeployDAO deployDAO;

    public GenericHealthCheck(TeletraanServiceContext context) {
        deployDAO = context.getDeployDAO();
    }

    @Override
    protected Result check() throws Exception {
        return Result.healthy("Teletraan looks healthy!");
    }
}
