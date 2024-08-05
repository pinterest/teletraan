/**
 * Copyright (c) 2016 Pinterest, Inc.
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
package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.ConfigHistoryBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.ConfigHistoryDAO;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

public class DBConfigHistoryDAOImpl implements ConfigHistoryDAO {
    private static final String INSERT_CONFIG_HISTORY = "INSERT INTO config_history SET %s";
    private static final String GET_ALL_BY_CONFIG_ID =
            "SELECT * FROM config_history WHERE config_id=? ORDER BY creation_time DESC LIMIT ?,?";
    private static final String GET_BY_CHANGE_ID = "SELECT * FROM config_history WHERE change_id=?";
    private static final String GET_LATEST_CHANGES_BY_TYPE =
            "SELECT * FROM config_history where config_id=? and type=? ORDER BY creation_time DESC LIMIT 2";

    private BasicDataSource dataSource;

    public DBConfigHistoryDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(ConfigHistoryBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_CONFIG_HISTORY, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public List<ConfigHistoryBean> getByConfigId(String configId, int pageIndex, int pageSize)
            throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        long start = (pageIndex - 1) * pageSize;
        ResultSetHandler<List<ConfigHistoryBean>> h =
                new BeanListHandler<ConfigHistoryBean>(ConfigHistoryBean.class);
        return run.query(GET_ALL_BY_CONFIG_ID, h, configId, start, pageSize);
    }

    @Override
    public ConfigHistoryBean getByChangeId(String changeId) throws Exception {
        ResultSetHandler<ConfigHistoryBean> h = new BeanHandler<>(ConfigHistoryBean.class);
        return new QueryRunner(dataSource).query(GET_BY_CHANGE_ID, h, changeId);
    }

    @Override
    public List<ConfigHistoryBean> getLatestChangesByType(String configId, String type)
            throws Exception {
        ResultSetHandler<List<ConfigHistoryBean>> h =
                new BeanListHandler<ConfigHistoryBean>(ConfigHistoryBean.class);
        return new QueryRunner(this.dataSource)
                .query(GET_LATEST_CHANGES_BY_TYPE, h, configId, type);
    }
}
