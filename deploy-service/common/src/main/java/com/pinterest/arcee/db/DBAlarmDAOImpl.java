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

import com.pinterest.arcee.bean.AsgAlarmBean;
import com.pinterest.arcee.bean.MetricBean;
import com.pinterest.deployservice.bean.AgentBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.arcee.dao.AlarmDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.List;

public class DBAlarmDAOImpl implements AlarmDAO {
    private static String GET_ALARM_BY_ID =
            "SELECT * FROM asg_alarms WHERE alarm_id=?";
    private static String GET_ALARM_BY_GROUP =
            "SELECT * FROM asg_alarms WHERE group_name=? ORDER BY metric_source";
    private static String GET_BY_GROUP_AND_METRICSOURCE =
        "SELECT * FROM asg_alarms WHERE group_name=? and metric_source=?";
    private static String INSERT_UPDATE_ALARM =
            "INSERT INTO asg_alarms SET %s ON DUPLICATE KEY UPDATE %s";
    private static String DELETE_ALARM_BY_ID =
            "DELETE FROM asg_alarms WHERE alarm_id=?";
    private static String DELETE_ALARM_BY_GROUP =
            "DELETE FROM asg_alarms WHERE group_name=?";
    private static String UPDATE_ALARM_BY_ID =
            "UPDATE asg_alarms SET %s WHERE alarm_id=?";
    private static String GET_ALL_METRICS =
            "SELECT DISTINCT metric_name, metric_source, group_name, from_aws_metric FROM asg_alarms";

    private BasicDataSource dataSource;
    public DBAlarmDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public AsgAlarmBean getAlarmInfoById(String alarmId) throws Exception {
        ResultSetHandler<AsgAlarmBean> h = new BeanHandler<AsgAlarmBean>(AsgAlarmBean.class);
        return new QueryRunner(dataSource).query(GET_ALARM_BY_ID, h, alarmId);
    }

    @Override
    public List<AsgAlarmBean> getAlarmInfosByGroup(String groupName) throws Exception {
        ResultSetHandler<List<AsgAlarmBean>> h = new BeanListHandler<AsgAlarmBean>(AsgAlarmBean.class);
        return new QueryRunner(dataSource).query(GET_ALARM_BY_GROUP, h, groupName);
    }

    @Override
    public AsgAlarmBean getAlarmInfoByGroupAndMetricSource(String groupName, String metricSource) throws Exception {
        ResultSetHandler<AsgAlarmBean> h = new BeanHandler<AsgAlarmBean>(AsgAlarmBean.class);
        return new QueryRunner(dataSource).query(GET_BY_GROUP_AND_METRICSOURCE, h, groupName, metricSource);
    }

    @Override
    public void insertOrUpdateAlarmInfo(AsgAlarmBean asgAlarmBean) throws Exception {
        asgAlarmBean.setLast_update(System.currentTimeMillis());
        SetClause setClause = asgAlarmBean.genSetClause();
        String clause = String.format(INSERT_UPDATE_ALARM, setClause.getClause(), AsgAlarmBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void deleteAlarmInfoById(String alarmId)  throws Exception {
        new QueryRunner(dataSource).update(DELETE_ALARM_BY_ID, alarmId);
    }


    @Override
    public void deleteAlarmInfoByGroup(String groupName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_ALARM_BY_GROUP, groupName);
    }

    @Override
    public void updateAlarmInfoById(String alarmId, AsgAlarmBean asgAlarmBean) throws Exception {
        SetClause setClause = asgAlarmBean.genSetClause();
        String clause = String.format(UPDATE_ALARM_BY_ID, setClause.getClause());
        setClause.addValue(alarmId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public List<MetricBean> getMetrics() throws Exception {
        ResultSetHandler<List<MetricBean>> h = new BeanListHandler<MetricBean>(MetricBean.class);
        return new QueryRunner(dataSource).query(GET_ALL_METRICS, h);
    }
}
