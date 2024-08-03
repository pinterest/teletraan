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

import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.dao.ScheduleDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBScheduleDAOImpl implements ScheduleDAO {
    private static final String INSERT_SCHEDULE = "INSERT INTO schedules SET %s";
    private static final String UPDATE_SCHEDULE = "UPDATE schedules SET %s WHERE id=?";
    private static final String DELETE_SCHEDULE = "DELETE FROM schedules WHERE id=?";
    private static final String GET_SCHEDULE_BY_ID = "SELECT * FROM schedules WHERE id=?";

    private static final Logger LOG = LoggerFactory.getLogger(DBScheduleDAOImpl.class);

    private BasicDataSource dataSource;

    public DBScheduleDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(ScheduleBean scheduleBean) throws Exception {
        SetClause setClause = scheduleBean.genSetClause();
        String clause = String.format(INSERT_SCHEDULE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(ScheduleBean scheduleBean, String scheduleId) throws Exception {
        SetClause setClause = scheduleBean.genSetClause();
        String clause = String.format(UPDATE_SCHEDULE, setClause.getClause());
        setClause.addValue(scheduleId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String scheduleId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_SCHEDULE, scheduleId);
    }

    @Override
    public ScheduleBean getById(String scheduleId) throws Exception {
        ResultSetHandler<ScheduleBean> h = new BeanHandler<ScheduleBean>(ScheduleBean.class);
        ScheduleBean bean = new QueryRunner(dataSource).query(GET_SCHEDULE_BY_ID, h, scheduleId);
        return bean;
    }
}
