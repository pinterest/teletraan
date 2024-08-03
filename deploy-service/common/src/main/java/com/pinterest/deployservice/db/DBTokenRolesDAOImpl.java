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
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

/** Implementation for UserRolesDAO. */
public class DBTokenRolesDAOImpl implements TokenRolesDAO {

    private static final String INSERT_TEMPLATE = "INSERT INTO tokens_and_roles SET %s";

    private static final String DELETE_TEMPLATE =
            "DELETE FROM tokens_and_roles WHERE script_name=? AND resource_id=? AND resource_type=?";

    private static final String UPDATE_TEMPLATE =
            "UPDATE tokens_and_roles SET %s WHERE script_name=? AND resource_id=? AND resource_type=?";

    private static final String GET_BY_TOKEN = "SELECT * FROM tokens_and_roles WHERE token=?";

    private static final String GET_BY_RESOURCE =
            "SELECT * FROM tokens_and_roles WHERE resource_id=? AND resource_type=? ORDER BY script_name";

    private static final String GET_BY_NAME_AND_RESOURCE =
            "SELECT * FROM tokens_and_roles WHERE script_name =? AND resource_id=? AND resource_type=?";

    private BasicDataSource dataSource;

    public DBTokenRolesDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(TokenRolesBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String userName, String resourceId, AuthZResource.Type resourceType)
            throws Exception {
        new QueryRunner(dataSource)
                .update(DELETE_TEMPLATE, userName, resourceId, resourceType.toString());
    }

    @Override
    public void update(
            TokenRolesBean bean,
            String userName,
            String resourceId,
            AuthZResource.Type resourceType)
            throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_TEMPLATE, setClause.getClause());
        setClause.addValue(userName);
        setClause.addValue(resourceId);
        setClause.addValue(resourceType.toString());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public TokenRolesBean getByToken(String token) throws Exception {
        ResultSetHandler<TokenRolesBean> h = new BeanHandler<>(TokenRolesBean.class);
        return new QueryRunner(dataSource).query(GET_BY_TOKEN, h, token);
    }

    @Override
    public TokenRolesBean getByNameAndResource(
            String userName, String resourceId, AuthZResource.Type resourceType) throws Exception {
        ResultSetHandler<TokenRolesBean> h = new BeanHandler<>(TokenRolesBean.class);
        return new QueryRunner(dataSource)
                .query(GET_BY_NAME_AND_RESOURCE, h, userName, resourceId, resourceType.toString());
    }

    @Override
    public List<TokenRolesBean> getByResource(String resourceId, AuthZResource.Type resourceType)
            throws Exception {
        ResultSetHandler<List<TokenRolesBean>> h = new BeanListHandler<>(TokenRolesBean.class);
        return new QueryRunner(dataSource)
                .query(GET_BY_RESOURCE, h, resourceId, resourceType.toString());
    }
}
