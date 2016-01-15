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
package com.pinterest.arcee.dao;


import com.pinterest.arcee.bean.HealthCheckBean;
import java.util.List;

public interface HealthCheckDAO {
    public void insertHealthCheck(HealthCheckBean healthCheckBean) throws Exception;

    public void updateHealthCheckById(String id, HealthCheckBean healthCheckBean) throws Exception;

    public void removeHealthCheckById(String id) throws Exception;

    public HealthCheckBean getHealthCheckById(String id) throws Exception;

    public List<HealthCheckBean> getHealthChecksByGroup(String groupName, int pageIndex, int pageSize) throws Exception;

    public List<HealthCheckBean> getOngoingHealthChecks() throws Exception;

    public List<HealthCheckBean> getOngoingRegularHealthChecksByGroup(String groupName) throws Exception;

    public List<HealthCheckBean> getRegularHealthChecksByGroupAndTime(String groupName, long time) throws Exception;

    public List<String> getRecentHealthCheckStatus(String groupName, int pageSize) throws Exception;

    public List<HealthCheckBean> getHealthChecksByUnterminatedHosts() throws Exception;
}
