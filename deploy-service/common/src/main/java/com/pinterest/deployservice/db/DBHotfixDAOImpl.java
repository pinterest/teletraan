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

import com.pinterest.deployservice.bean.HotfixBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.dao.HotfixDAO;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

/** Implementation for Hotfix DAO */
public class DBHotfixDAOImpl implements HotfixDAO {
    private static final String INSERT_HOTFIX_TEMPLATE = "INSERT INTO hotfixes SET %s";
    private static final String UPDATE_HOTFIX_BY_ID_TEMPLATE = "UPDATE hotfixes SET %s WHERE id=?";
    private static final String DELETE_HOTFIX = "DELETE FROM hotfixes WHERE job_num=?";
    private static final String GET_HOTFIX_BY_ID = "SELECT * FROM hotfixes WHERE id=?";
    private static final String GET_ONGOING_HOTFIX_IDS_TEMPLATE =
            "SELECT id FROM hotfixes WHERE state IN (%s)";
    private static final String GET_HOTFIXES =
            "SELECT * FROM hotfixes WHERE env_name=? ORDER BY start_time DESC LIMIT ?,?";

    private BasicDataSource dataSource;

    public DBHotfixDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(HotfixBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_HOTFIX_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void update(String hotfix_id, HotfixBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_HOTFIX_BY_ID_TEMPLATE, setClause.getClause());
        setClause.addValue(hotfix_id);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HOTFIX, id);
    }

    @Override
    public List<String> getOngoingHotfixIds() throws Exception {
        String statesClause = QueryUtils.genEnumGroupClause(StateMachines.HOTFIX_ONGOING_STATES);
        return new QueryRunner(dataSource)
                .query(
                        String.format(GET_ONGOING_HOTFIX_IDS_TEMPLATE, statesClause),
                        SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }

    @Override
    public List<HotfixBean> getHotfixes(String envName, int pageIndex, int pageSize)
            throws Exception {
        ResultSetHandler<List<HotfixBean>> h = new BeanListHandler<>(HotfixBean.class);
        QueryRunner run = new QueryRunner(this.dataSource);
        return run.query(GET_HOTFIXES, h, envName, (pageIndex - 1) * pageSize, pageSize);
    }

    @Override
    public HotfixBean getByHotfixId(String hotfix_id) throws Exception {
        ResultSetHandler<HotfixBean> h = new BeanHandler<>(HotfixBean.class);
        return new QueryRunner(dataSource).query(GET_HOTFIX_BY_ID, h, hotfix_id);
    }
}
