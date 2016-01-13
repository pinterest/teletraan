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
package com.pinterest.arcee.db;

import com.pinterest.arcee.bean.NewInstanceReportBean;
import com.pinterest.arcee.dao.NewInstanceReportDAO;
import com.pinterest.deployservice.db.SingleResultSetHandlerFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

import java.util.Collection;
import java.util.List;


public class DBNewInstanceReportDAOImpl implements NewInstanceReportDAO {

    private static final String UPDATE_INSTANCE_REPORT_TMPL =
            "UPDATE new_instances_reports SET reported=1 WHERE host_id=? AND env_id=?";

    private static final String DELETE_INSTANCE_REPORT_TMPL =
            "DELETE FROM new_instances_reports WHERE host_id=? AND env_id=?";

    private static final String GET_BY_IDS =
            "SELECT * FROM new_instances_reports WHERE host_id=? AND env_id=?";

    private static final String ADD_NEW_REPORTS = "INSERT INTO new_instances_reports %s VALUES %s ON DUPLICATE KEY UPDATE launch_time=?";

    private static final String GET_IDS_BY_ENV =
            "SELECT host_id FROM new_instances_reports WHERE env_id=?";

    private BasicDataSource dataSource;
    public DBNewInstanceReportDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public NewInstanceReportBean getByIds(String hostId, String envId) throws Exception {
        ResultSetHandler<NewInstanceReportBean> h = new BeanHandler<>(NewInstanceReportBean.class);
        return new QueryRunner(dataSource).query(GET_BY_IDS, h, hostId, envId);
    }

    @Override
    public void reportNewInstances(String hostId, String envId) throws Exception {
        new QueryRunner(dataSource).update(UPDATE_INSTANCE_REPORT_TMPL, hostId, envId);
    }

    @Override
    public void deleteNewInstanceReport(String hostId, String envId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_INSTANCE_REPORT_TMPL, hostId, envId);
    }

    @Override
    public void addNewInstanceReport(String hostId, Long launchTime, Collection<String> envIds) throws Exception {
        StringBuilder names = new StringBuilder("(host_id,env_id,launch_time)");

        StringBuilder sb = new StringBuilder();
        for (String envId : envIds) {
            sb.append("('");
            sb.append(hostId);
            sb.append("','");
            sb.append(envId);
            sb.append("',");
            sb.append(launchTime);
            sb.append("),");
        }
        sb.setLength(sb.length() - 1);
        new QueryRunner(dataSource).update(String.format(ADD_NEW_REPORTS, names, sb.toString()), launchTime);
    }

    @Override
    public List<String> getNewInstanceIdsByEnv(String envId) throws Exception {
        return new QueryRunner(dataSource).query(GET_IDS_BY_ENV,
                SingleResultSetHandlerFactory.<String>newListObjectHandler(), envId);
    }
}
