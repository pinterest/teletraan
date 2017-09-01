package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.DeployRuleBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.DeployRuleDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

/**
 * Created by suli on 8/31/17.
 */
public class DBDeployRuleDAOImpl implements DeployRuleDAO {
    private static final String INSERT_RULE_TEMPLATE = "INSERT INTO deploy_rules SET %s";

    private static final String GET_RULE_BY_ID = "SELECT * FROM deploy_rules WHERE rule_id = ?";

    private static final String REMOVE_RULE_BY_ID = "DELETE FROM deploy_rules WHERE rule_id = ?";


    private BasicDataSource dataSource;

    public DBDeployRuleDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }


    public DeployRuleBean getById(String ruleId) throws Exception {
        ResultSetHandler<DeployRuleBean> h = new BeanHandler<DeployRuleBean>(DeployRuleBean.class);
        return new QueryRunner(dataSource).query(GET_RULE_BY_ID, h, ruleId);
    }

    public void insert(DeployRuleBean deployRuleBean) throws Exception {
        SetClause setClause = deployRuleBean.genSetClause();
        String clause = String.format(INSERT_RULE_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    public void delete(String ruleId) throws Exception {
        new QueryRunner(dataSource).update(REMOVE_RULE_BY_ID, ruleId);
    }
}
