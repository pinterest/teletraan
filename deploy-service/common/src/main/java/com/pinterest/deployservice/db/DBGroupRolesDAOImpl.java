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

import com.pinterest.deployservice.bean.GroupRolesBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

/** Implementation for GroupRolesDAO. */
public class DBGroupRolesDAOImpl implements GroupRolesDAO {

    private static final String INSERT_TEMPLATE = "INSERT INTO groups_and_roles SET %s";

    private static final String DELETE_TEMPLATE =
            "DELETE FROM groups_and_roles WHERE group_name=? "
                    + "AND resource_id=? AND resource_type=?";

    private static final String UPDATE_TEMPLATE =
            "UPDATE groups_and_roles SET %s WHERE group_name=? "
                    + "AND resource_id=? AND resource_type=?";

    private static final String GET_BY_RESOURCE =
            "SELECT * FROM groups_and_roles WHERE resource_id=? "
                    + "AND resource_type=? ORDER BY group_name";

    private static final String GET_BY_NAME_AND_RESOURCE =
            "SELECT * FROM groups_and_roles WHERE group_name =? "
                    + "AND resource_id=? AND resource_type=? ORDER BY role";

    private BasicDataSource dataSource;

    public DBGroupRolesDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(GroupRolesBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String groupName, String resourceId, AuthZResource.Type resourceType)
            throws Exception {
        new QueryRunner(dataSource)
                .update(DELETE_TEMPLATE, groupName, resourceId, resourceType.toString());
    }

    @Override
    public void update(
            GroupRolesBean bean,
            String groupName,
            String resourceId,
            AuthZResource.Type resourceType)
            throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_TEMPLATE, setClause.getClause());
        setClause.addValue(groupName);
        setClause.addValue(resourceId);
        setClause.addValue(resourceType.toString());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public GroupRolesBean getByNameAndResource(
            String groupName, String resourceId, AuthZResource.Type resourceType) throws Exception {
        ResultSetHandler<GroupRolesBean> h = new BeanHandler<>(GroupRolesBean.class);
        return new QueryRunner(dataSource)
                .query(GET_BY_NAME_AND_RESOURCE, h, groupName, resourceId, resourceType.toString());
    }

    @Override
    public List<GroupRolesBean> getByResource(String resourceId, AuthZResource.Type resourceType)
            throws Exception {
        ResultSetHandler<List<GroupRolesBean>> h = new BeanListHandler<>(GroupRolesBean.class);
        return new QueryRunner(dataSource)
                .query(GET_BY_RESOURCE, h, resourceId, resourceType.toString());
    }
}
