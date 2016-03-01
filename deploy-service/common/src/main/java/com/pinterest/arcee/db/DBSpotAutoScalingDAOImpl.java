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


import com.pinterest.arcee.bean.SpotAutoScalingBean;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.deployservice.bean.SetClause;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.List;

public class DBSpotAutoScalingDAOImpl implements SpotAutoScalingDAO {
    private static String GET_CLUSTER_BY_ID =
            "SELECT * FROM spot_auto_scaling_groups WHERE asg_name=?";
    private static String GET_ASG_BY_CLUSTER =
            "SELECT * FROM spot_auto_scaling_groups WHERE cluster_name=?";
    private static String INSERT_UPDATE_ASG_MAPPING =
            "INSERT INTO spot_auto_scaling_groups SET %s ON DUPLICATE KEY UPDATE %s";
    private static String DELETE_ASG_BY_NAME =
            "DELETE FROM spot_auto_scaling_groups WHERE asg_name=?";
    private static String DELETE_ASG_BY_CLUSTER =
            "DELETE FROM spot_auto_scaling_groups WHERE cluster_name=?";
    private static String UPDATE_ASG_BY_CLUSTER =
            "UPDATE spot_auto_scaling_groups SET %s WHERE asg_name=?";
    private static String GET_ALL_CLUSTER =
            "SELECT * FROM spot_auto_scaling_groups";

    private BasicDataSource dataSource;
    public DBSpotAutoScalingDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public SpotAutoScalingBean getClusterByAutoScalingGroup(String autoScalingGroup) throws Exception {
        ResultSetHandler<SpotAutoScalingBean> h = new BeanHandler<>(SpotAutoScalingBean.class);
        return new QueryRunner(dataSource).query(GET_CLUSTER_BY_ID, h, autoScalingGroup);
    }

    @Override
    public List<SpotAutoScalingBean> getAutoScalingGroupsByCluster(String clusterName) throws Exception {
        ResultSetHandler<List<SpotAutoScalingBean>> h = new BeanListHandler<>(SpotAutoScalingBean.class);
        return new QueryRunner(dataSource).query(GET_ASG_BY_CLUSTER, h, clusterName);
    }

    @Override
    public void insertAutoScalingGroupToCluster(String autoScalingGroup, SpotAutoScalingBean spotAutoScalingBean) throws Exception {
        SetClause setClause = spotAutoScalingBean.genSetClause();
        String clause = String.format(INSERT_UPDATE_ASG_MAPPING, setClause.getClause(), spotAutoScalingBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void deleteAutoScalingGroupFromCluster(String autoScalingGroup) throws Exception {
        new QueryRunner(dataSource).update(DELETE_ASG_BY_NAME, autoScalingGroup);
    }

    @Override
    public void deleteAllAutoScalingGroupByCluster(String clusterName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_ASG_BY_CLUSTER, clusterName);
    }

    @Override
    public void updateSpotAutoScalingGroup(String groupName, SpotAutoScalingBean spotAutoScalingBean) throws Exception {
        SetClause setClause = spotAutoScalingBean.genSetClause();
        String clause = String.format(UPDATE_ASG_BY_CLUSTER, setClause.getClause());
        setClause.addValue(groupName);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public List<SpotAutoScalingBean> getAllSpotAutoScalingGroups() throws Exception {
        ResultSetHandler<List<SpotAutoScalingBean>> h = new BeanListHandler<>(SpotAutoScalingBean.class);
        return new QueryRunner(dataSource).query(GET_ALL_CLUSTER, h);
    }
}
