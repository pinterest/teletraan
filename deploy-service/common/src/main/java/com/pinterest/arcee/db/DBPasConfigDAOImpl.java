package com.pinterest.arcee.db;

import com.pinterest.arcee.bean.PasConfigBean;
import com.pinterest.arcee.dao.PasConfigDAO;
import com.pinterest.deployservice.bean.SetClause;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBPasConfigDAOImpl implements PasConfigDAO {

    private static final Logger LOG = LoggerFactory.getLogger(DBPasConfigDAOImpl.class);

    private final String INSERT_PAS_CONFIG = "INSERT INTO pas_configs SET %s ON DUPLICATE KEY UPDATE %s";

    private final String GET_PAS_CONFIG = "SELECT * FROM pas_configs WHERE group_name=?";

    private final String UPDATE_PAS_CONFIG = "UPDATE pas_configs SET %s WHERE group_name=?";

    private BasicDataSource dataSource;

    public DBPasConfigDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertPasConfig(PasConfigBean pasConfigBean) throws Exception {
        if (pasConfigBean.getLast_updated() == null) {
            pasConfigBean.setLast_updated(System.currentTimeMillis());
        }
        SetClause setClause = pasConfigBean.genSetClause();
        String clause = String.format(INSERT_PAS_CONFIG, setClause.getClause(), PasConfigBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void updatePasConfig(PasConfigBean pasConfigBean) throws Exception {
        if (pasConfigBean.getLast_updated() == null) {
            pasConfigBean.setLast_updated(System.currentTimeMillis());
        }
        SetClause setClause = pasConfigBean.genSetClause();
        String clause = String.format(UPDATE_PAS_CONFIG, setClause.getClause());
        setClause.addValue(pasConfigBean.getGroup_name());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());

    }

    @Override
    public PasConfigBean getPasConfig(String groupName) throws Exception {
        ResultSetHandler<PasConfigBean> h = new BeanHandler<>(PasConfigBean.class);
        return new QueryRunner(dataSource).query(GET_PAS_CONFIG, h, groupName);
    }
}
