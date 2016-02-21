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


import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.deployservice.db.SingleResultSetHandlerFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import java.util.List;

public class DBGroupInfoDAOImpl implements GroupInfoDAO {

    private static final String INSERT_GROUP_INFO = "INSERT INTO groups SET %s;";

    private static final String UPDATE_GROUP_INFO = "UPDATE groups SET %s WHERE group_name=?;";

    private static final String DELETE_GROUP_BY_NAME = "DELETE FROM groups WHERE group_name=?;";

    private static final String GET_GROUP_INFO_BY_NAME = "SELECT * FROM groups WHERE group_name=?;";

    private static final String GET_NEW_GROUP_INFO = "SELECT DISTINCT group_name FROM groups_and_envs WHERE group_name NOT IN (SELECT group_name FROM groups)";

    private static final String GET_GROUP_NAMES = "SELECT DISTINCT group_name FROM groups LIMIT ?,?;";

    private static final String GET_TO_UPDATE_GROUPS_NAME = "SELECT group_name FROM groups WHERE last_update<?";

    private static final String GET_GROUP_INFO_BY_APP_NAME = "SELECT groups.* FROM groups INNER JOIN images ON groups.image_id=images.id WHERE images.app_name=?";

    private static final String GET_GROUPNAMES_BY_HOSTID_AND_ASGSTATUS =
        "SELECT g.group_name FROM groups g INNER JOIN hosts h ON h.group_name = g.group_name WHERE h.host_id=? AND g.asg_status=?";

    private static final String GET_GROUPNAMES_BY_ENVNAME_AND_ASGSTATUS =
        "SELECT g.group_name FROM groups g INNER JOIN (SELECT ge.group_name FROM groups_and_envs ge INNER JOIN environs e ON e.env_id = ge.env_id and e.env_name=?)" +
            " gee ON gee.group_name = g.group_name and g.asg_status=?;";

    private static final String GET_GROUPS_BY_ENVNAME_AND_ASGSTATUS =
        "SELECT g.* FROM groups g INNER JOIN (SELECT ge.group_name FROM groups_and_envs ge INNER JOIN environs e ON e.env_id = ge.env_id and e.env_name=?)" +
            " gee ON gee.group_name = g.group_name and g.asg_status=?;";

    private static final String GET_ENABLED_HEALTHCHECK_GROUP_NAMES = "SELECT DISTINCT group_name FROM groups WHERE healthcheck_state=1";

    private BasicDataSource dataSource;

    public DBGroupInfoDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void updateGroupInfo(String groupName, GroupBean bean) throws Exception {
        if (bean.getLast_update() == null) {
            bean.setLast_update(System.currentTimeMillis());
        }
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_GROUP_INFO, setClause.getClause());
        setClause.addValue(groupName);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void insertGroupInfo(GroupBean groupBean) throws Exception {
        if (groupBean.getLast_update() == null) {
            groupBean.setLast_update(System.currentTimeMillis());
        }
        SetClause setClause = groupBean.genSetClause();
        String clause = String.format(INSERT_GROUP_INFO, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public GroupBean getGroupInfo(String groupName) throws Exception {
        ResultSetHandler<GroupBean> h = new BeanHandler<GroupBean>(GroupBean.class);
        return new QueryRunner(dataSource).query(GET_GROUP_INFO_BY_NAME, h, groupName);
    }

    @Override
    public void removeGroup(String groupName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_GROUP_BY_NAME, groupName);
    }

    @Override
    public List<String> getExistingGroups(long pageIndex, int pageSize) throws Exception {
        return new QueryRunner(dataSource).query(GET_GROUP_NAMES,
                SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                (pageIndex - 1) * pageSize, pageSize);
    }

    @Override
    public List<String> getToUpdateGroups(long after) throws Exception {
        return new QueryRunner(dataSource).query(GET_TO_UPDATE_GROUPS_NAME,
                SingleResultSetHandlerFactory.<String>newListObjectHandler(), after);
    }

    @Override
    public List<String> getNewGroupNames() throws Exception {
        return new QueryRunner(dataSource).query(GET_NEW_GROUP_INFO, SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }

    @Override
    public List<GroupBean> getGroupInfoByAppName(String appName) throws Exception {
        ResultSetHandler<List<GroupBean>> h = new BeanListHandler<GroupBean>(GroupBean.class);
        return new QueryRunner(dataSource).query(GET_GROUP_INFO_BY_APP_NAME, h, appName);
    }

    @Override
    public List<String> getGroupNamesByHostIdAndASGStatus(String hostId, String asgStatus) throws Exception {
        return new QueryRunner(dataSource).query(GET_GROUPNAMES_BY_HOSTID_AND_ASGSTATUS,
            SingleResultSetHandlerFactory.<String>newListObjectHandler(), hostId, asgStatus);
    }

    @Override
    public List<String> getGroupNamesByEnvNameAndASGStatus(String envName, String asgStatus) throws Exception {
        return new QueryRunner(dataSource).query(GET_GROUPNAMES_BY_ENVNAME_AND_ASGSTATUS,
            SingleResultSetHandlerFactory.<String>newListObjectHandler(), envName, asgStatus);
    }

    @Override
    public List<GroupBean> getGroupsByEnvNameAndASGStauts(String envName, String asgStatus) throws Exception {
        ResultSetHandler<List<GroupBean>> h = new BeanListHandler<>(GroupBean.class);
        return new QueryRunner(dataSource).query(GET_GROUPS_BY_ENVNAME_AND_ASGSTATUS, h, envName, asgStatus);
    }

    @Override
    public List<String> getEnabledHealthCheckGroupNames() throws Exception {
        return new QueryRunner(dataSource).query(GET_ENABLED_HEALTHCHECK_GROUP_NAMES, SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }
}

