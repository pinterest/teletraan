/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.WorkerJobBean;
import com.pinterest.deployservice.dao.WorkerJobDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

public class DBWorkerJobsDAOImpl implements WorkerJobDAO {
    private static final String INSERT_ENV_TEMPLATE = "INSERT INTO worker_jobs SET %s";
    private static final String GET_ENV_BY_ID = "SELECT * FROM worker_jobs WHERE id=?";

    private BasicDataSource dataSource;

    public DBWorkerJobsDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(WorkerJobBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_ENV_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public WorkerJobBean getById(String envId) throws Exception {
        ResultSetHandler<WorkerJobBean> h = new BeanHandler<>(WorkerJobBean.class);
        return new QueryRunner(dataSource).query(GET_ENV_BY_ID, h, envId);
    }
}
