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
package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.List;

/**
 * Implementation for UserRolesDAO.
 */
public class DBUserRolesDAOImpl implements UserRolesDAO {

    private static final String INSERT_TEMPLATE =
        "INSERT INTO users_and_roles SET %s ON DUPLICATE KEY UPDATE %s";

    private static final String DELETE_TEMPLATE =
        "DELETE FROM users_and_roles WHERE user_name=? AND resource_id=? AND resource_type=?";

    private static final String UPDATE_TEMPLATE =
        "UPDATE users_and_roles SET %s WHERE user_name=? AND resource_id=? AND resource_type=?";

    private static final String GET_BY_RESOURCE =
        "SELECT * FROM users_and_roles WHERE resource_id=? AND resource_type=? ORDER BY user_name";

    private static final String GET_BY_NAME_AND_RESOURCE =
        "SELECT * FROM users_and_roles WHERE user_name =? AND resource_id=? AND resource_type=? ORDER BY role";

    private BasicDataSource dataSource;

    public DBUserRolesDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(UserRolesBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_TEMPLATE, setClause.getClause(), UserRolesBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String userName, String resourceId,
        AuthZResource.Type resourceType) throws Exception {
        new QueryRunner(dataSource).update(DELETE_TEMPLATE, userName, resourceId,
            resourceType.toString());
    }

    @Override
    public void update(UserRolesBean bean, String userName, String resourceId,
        AuthZResource.Type resourceType) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_TEMPLATE, setClause.getClause());
        setClause.addValue(userName);
        setClause.addValue(resourceId);
        setClause.addValue(resourceType.toString());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public UserRolesBean getByNameAndResource(String userName, String resourceId,
        AuthZResource.Type resourceType) throws Exception {
        ResultSetHandler<UserRolesBean> h = new BeanHandler<>(UserRolesBean.class);
        return new QueryRunner(dataSource).query(GET_BY_NAME_AND_RESOURCE, h, userName,
            resourceId, resourceType.toString());
    }

    @Override
    public List<UserRolesBean> getByResource(String resourceId,
        AuthZResource.Type resourceType) throws Exception {
        ResultSetHandler<List<UserRolesBean>> h = new BeanListHandler<>(UserRolesBean.class);
        return new QueryRunner(dataSource).query(GET_BY_RESOURCE, h, resourceId,
            resourceType.toString());
    }
}
