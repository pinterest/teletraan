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

import com.pinterest.arcee.bean.AsgLifecycleEventBean;
import com.pinterest.arcee.dao.AsgLifecycleEventDAO;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.db.SingleResultSetHandlerFactory;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import java.util.List;

public class DBAsgLifecycleEventDAOImpl implements AsgLifecycleEventDAO {

    private static String INSERT_ASG_LIFE_CYCLE_EVENT = "INSERT INTO asg_lifecycle_events SET %s";

    private static String DELETE_ASG_LIFE_CYCLE_EVENT_BY_ID = "DELETE FROM asg_lifecycle_events WHERE token_id=?";

    private static String DELETE_ASG_LIFE_CYCLE_EVENT_BY_HOOKID = "DELETE FROM asg_lifecycle_events WHERE hook_id=?";

    private static String GET_ASG_LIFE_CYCLE_EVENT_BY_HOOK = "SELECT * FROM asg_lifecycle_events WHERE hook_id=?";

    private static String GET_HOOKID_FROM_ASG_LIFE_CYCLE_EVENT = "SELECT DISTINCT hook_id FROM asg_lifecycle_events";

    private BasicDataSource dataSource;

    public DBAsgLifecycleEventDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertAsgLifecycleEvent(AsgLifecycleEventBean asgLifecycleEventBean) throws Exception {
        SetClause setClause = asgLifecycleEventBean.genSetClause();
        String clause = String.format(INSERT_ASG_LIFE_CYCLE_EVENT, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void deleteAsgLifecycleEventById(String tokenId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_ASG_LIFE_CYCLE_EVENT_BY_ID, tokenId);
    }

    @Override
    public void deleteAsgLifeCycleEventByHookId(String hookId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_ASG_LIFE_CYCLE_EVENT_BY_HOOKID, hookId);
    }

    @Override
    public List<AsgLifecycleEventBean> getAsgLifecycleEventByHook(String hookId) throws Exception {
        ResultSetHandler<List<AsgLifecycleEventBean>> h  = new BeanListHandler<AsgLifecycleEventBean>(AsgLifecycleEventBean.class);
        return new QueryRunner(dataSource).query(GET_ASG_LIFE_CYCLE_EVENT_BY_HOOK, h, hookId);
    }

    @Override
    public List<String> getHookIdsFromAsgLifeCycleEvent() throws Exception {
        return new QueryRunner(dataSource).query(GET_HOOKID_FROM_ASG_LIFE_CYCLE_EVENT,
                SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }
}
