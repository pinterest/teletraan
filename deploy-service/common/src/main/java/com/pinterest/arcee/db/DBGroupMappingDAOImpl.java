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

import com.pinterest.arcee.bean.GroupMappingBean;
import com.pinterest.arcee.dao.GroupMappingDAO;
import com.pinterest.deployservice.bean.SetClause;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.Collection;
import java.util.List;

public class DBGroupMappingDAOImpl implements GroupMappingDAO {
    private static final String INSERT_GROUP_MAPPING = "INSERT INTO group_mappings SET %s ON DUPLICATE KEY UPDATE %s";

    private static final String UPDATE_GROUP_MAPPING = "UPDATE group_mappings SET %s WHERE asg_group_name=?";

    private static final String GET_GROUP_MAPPING_BY_CLUSTER = "SELECT * FROM group_mappings WHERE cluster_name=?";

    private static final String GET_GROUP_MAPPING_BY_ASG = "SELECT * FROM group_mappings WHERE asg_group_name=?";

    private static final String DELTE_GROUP_MAPPING = "DELETE FROM group_mappings WHERE asg_group_name=?";

    private BasicDataSource dataSource;

    public DBGroupMappingDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertGroupMapping(String asgGroupName, GroupMappingBean groupMappingsBean)
        throws Exception {
        SetClause setClause = groupMappingsBean.genSetClause();
        String clause = String.format(INSERT_GROUP_MAPPING, setClause.getClause(), GroupMappingBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void updateGroupMapping(String asgGroupName, GroupMappingBean groupMappingsBean)
        throws Exception {
        SetClause setClause = groupMappingsBean.genSetClause();
        String clause = String.format(UPDATE_GROUP_MAPPING, setClause.getClause());
        setClause.addValue(asgGroupName);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void deleteGroupMapping(String asgGroupName) throws Exception {
        new QueryRunner(dataSource).update(DELTE_GROUP_MAPPING, asgGroupName);
    }

    @Override
    public GroupMappingBean getGroupMappingByAsgGroupName(String asgGroupName) throws Exception {
        ResultSetHandler<GroupMappingBean> h = new BeanHandler<>(GroupMappingBean.class);
        return new QueryRunner(dataSource).query(GET_GROUP_MAPPING_BY_ASG, h, asgGroupName);
    }

    @Override
    public Collection<GroupMappingBean> getGroupMappingsByCluster(String clusterName) throws Exception {
        ResultSetHandler<List<GroupMappingBean>> h = new BeanListHandler<>(GroupMappingBean.class);
        return new QueryRunner(dataSource).query(GET_GROUP_MAPPING_BY_CLUSTER, h, clusterName);
    }
}
