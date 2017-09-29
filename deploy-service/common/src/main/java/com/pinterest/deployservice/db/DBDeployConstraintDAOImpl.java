package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.DeployConstraintBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.TagSyncState;
import com.pinterest.deployservice.bean.UpdateStatement;
import com.pinterest.deployservice.dao.DeployConstraintDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.util.List;


public class DBDeployConstraintDAOImpl implements DeployConstraintDAO {
    private static final String INSERT_CONSTRAINT_TEMPLATE = "INSERT INTO deploy_constraints SET %s";

    private static final String UPDATE_CONSTRAINT_TEMPLATE = "UPDATE deploy_constraints SET %s WHERE constraint_id=?";

    private static final String GET_CONSTRAINT_BY_ID = "SELECT * FROM deploy_constraints WHERE constraint_id = ?";

    private static final String REMOVE_CONSTRAINT_BY_ID = "DELETE FROM deploy_constraints WHERE constraint_id = ?";

    private static final String GET_ALL_ACTIVE = "SELECT * FROM deploy_constraints WHERE state != ?";

    private BasicDataSource dataSource;

    public DBDeployConstraintDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DeployConstraintBean getById(String constraintId) throws Exception {
        ResultSetHandler<DeployConstraintBean> h = new BeanHandler<>(DeployConstraintBean.class);
        return new QueryRunner(dataSource).query(GET_CONSTRAINT_BY_ID, h, constraintId);
    }

    @Override
    public void updateById(String constraintId, DeployConstraintBean deployConstraintBean) throws Exception {
        SetClause setClause = deployConstraintBean.genSetClause();
        String clause = String.format(UPDATE_CONSTRAINT_TEMPLATE, setClause.getClause());
        setClause.addValue(constraintId);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public UpdateStatement genInsertStatement(DeployConstraintBean deployConstraintBean) {
        SetClause setClause = deployConstraintBean.genSetClause();
        String clause = String.format(INSERT_CONSTRAINT_TEMPLATE, setClause.getClause());
        return new UpdateStatement(clause, setClause.getValueArray());
    }

    @Override
    public UpdateStatement genUpdateStatement(String constraintId, DeployConstraintBean deployConstraintBean) {
        SetClause setClause = deployConstraintBean.genSetClause();
        String clause = String.format(UPDATE_CONSTRAINT_TEMPLATE, setClause.getClause());
        setClause.addValue(constraintId);
        return new UpdateStatement(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String constraintId) throws Exception {
        new QueryRunner(dataSource).update(REMOVE_CONSTRAINT_BY_ID, constraintId);
    }

    @Override
    public List<DeployConstraintBean> getAllActiveDeployConstraint() throws Exception {
        ResultSetHandler<List<DeployConstraintBean>> h = new BeanListHandler<DeployConstraintBean>(DeployConstraintBean.class);
        return new QueryRunner(dataSource).query(GET_ALL_ACTIVE, h, TagSyncState.ERROR.toString());
    }
}
