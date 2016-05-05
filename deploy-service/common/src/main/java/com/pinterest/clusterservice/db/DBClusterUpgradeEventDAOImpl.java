package com.pinterest.clusterservice.db;


import com.pinterest.clusterservice.bean.ClusterUpgradeEventBean;
import com.pinterest.clusterservice.bean.ClusterUpgradeEventState;
import com.pinterest.clusterservice.dao.ClusterUpgradeEventDAO;
import com.pinterest.deployservice.bean.SetClause;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.Collection;
import java.util.List;

public class DBClusterUpgradeEventDAOImpl implements ClusterUpgradeEventDAO {

    private static String INSERT_CLUSTER_EVENT = "INSERT INTO cluster_upgrade_events SET %s";

    private static String UPDATE_BY_ID = "UPDATE cluster_upgrade_events SET %s WHERE id=?";

    private static String GET_BY_ID = "SELECT * FROM cluster_upgrade_events WHERE id=?";

    private static String GET_ONGOING_EVENTS = "SELECT * FROM cluster_upgrade_events WHERE state!=?";

    private static String GET_ONGOING_EVENTS_BY_CLUSTER = "SELECT * FROM cluster_upgrade_events WHERE cluster_name=? AND state!=?";

    private BasicDataSource dataSource;

    public DBClusterUpgradeEventDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertClusterUpgradeEvent(ClusterUpgradeEventBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_CLUSTER_EVENT, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void updateById(String id, ClusterUpgradeEventBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(UPDATE_BY_ID, setClause.getClause());
        setClause.addValue(id);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public ClusterUpgradeEventBean getById(String id) throws Exception {
        ResultSetHandler<ClusterUpgradeEventBean> h = new BeanHandler<ClusterUpgradeEventBean>(ClusterUpgradeEventBean.class);
        return new QueryRunner(dataSource).query(GET_BY_ID, h, id);
    }

    @Override
    public Collection<ClusterUpgradeEventBean> getOngoingEvents() throws Exception {
        ResultSetHandler<List<ClusterUpgradeEventBean>> h = new BeanListHandler<ClusterUpgradeEventBean>(ClusterUpgradeEventBean.class);
        return new QueryRunner(dataSource).query(GET_ONGOING_EVENTS, h, ClusterUpgradeEventState.COMPLETED.toString());
    }

    @Override
    public Collection<ClusterUpgradeEventBean> getOngoingEventsByCluster(String clusterName) throws Exception {
        ResultSetHandler<List<ClusterUpgradeEventBean>> h = new BeanListHandler<ClusterUpgradeEventBean>(ClusterUpgradeEventBean.class);
        return new QueryRunner(dataSource).query(GET_ONGOING_EVENTS_BY_CLUSTER, h, clusterName, ClusterUpgradeEventState.COMPLETED.toString());
    }
}
