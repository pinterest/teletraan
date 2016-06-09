package com.pinterest.arcee.db;


import com.pinterest.arcee.bean.ManagingGroupsBean;
import com.pinterest.arcee.dao.ManagingGroupDAO;
import com.pinterest.deployservice.bean.SetClause;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.List;

public class DBManaginGroupDAOImpl implements ManagingGroupDAO {
    private static String INSERT_MANAGING_GROUP_BY_ID =
            "INSERT INTO managing_groups SET %s ON DUPLICATE KEY UPDATE %s";
    private static String GET_MANAGING_GROUP_BY_ID =
            "SELECT * FROM managing_groups WHERE group_name=?";
    private static String GET_LENDING_MANAGING_GROUP_BY_TYPE =
            "SELECT * FROM managing_groups WHERE instance_type=? ORDER BY lending_priority, lent_size";
    private static String GET_RETURN_MANAGING_GROUP_BY_TYPE =
            "SELECT * FROM managing_groups WHERE instance_type=? ORDER BY lending_priority DESC, lent_size DESC";
    private static String UPDATE_MANAGING_GROUP_BY_ID =
            "UPDATE managing_groups SET %s WHERE group_name=?";
    private static String DELETE_MANAGING_GROUP_BY_ID =
            "DELETE FROM managing_groups WHERE group_name=?";
    private BasicDataSource dataSource;

    public DBManaginGroupDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }
    @Override
    public ManagingGroupsBean getManagingGroupByGroupName(String groupName) throws Exception {
        ResultSetHandler<ManagingGroupsBean> h = new BeanHandler<>(ManagingGroupsBean.class);
        return new QueryRunner(dataSource).query(GET_MANAGING_GROUP_BY_ID, h, groupName);
    }

    @Override
    public void updateManagingGroup(String groupName, ManagingGroupsBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_MANAGING_GROUP_BY_ID, setClause.getClause());
        setClause.addValue(groupName);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void deleteManagingGroup(String groupName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_MANAGING_GROUP_BY_ID, groupName);
    }

    @Override
    public void insertManagingGroup(String clusterName, ManagingGroupsBean managingGroupsBean) throws Exception {
        if (managingGroupsBean.getLast_activity_time() == null) {
            managingGroupsBean.setLast_activity_time(System.currentTimeMillis());
        }
        SetClause setClause = managingGroupsBean.genSetClause();
        managingGroupsBean.setGroup_name(clusterName);
        String clause = String.format(INSERT_MANAGING_GROUP_BY_ID, setClause.getClause(), ManagingGroupsBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public List<ManagingGroupsBean> getLendManagingGroupsByInstanceType(String instanceType) throws Exception {
        ResultSetHandler<List<ManagingGroupsBean>> h = new BeanListHandler<>(ManagingGroupsBean.class);
        return new QueryRunner(dataSource).query(GET_LENDING_MANAGING_GROUP_BY_TYPE, h, instanceType);
    }

    @Override
    public List<ManagingGroupsBean> getReturnManagingGroupsByInstanceType(String instanceType) throws Exception {
        ResultSetHandler<List<ManagingGroupsBean>> h = new BeanListHandler<>(ManagingGroupsBean.class);
        return new QueryRunner(dataSource).query(GET_RETURN_MANAGING_GROUP_BY_TYPE, h, instanceType);
    }
}
