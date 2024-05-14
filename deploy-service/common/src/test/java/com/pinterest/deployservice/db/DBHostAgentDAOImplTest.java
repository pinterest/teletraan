package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.EnvironBean;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.MySQLContainer;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DBHostAgentDAOImplTest {

    private DBHostAgentDAOImpl hostAgentDAO;
    public static final MySQLContainer<?> mysql = DBUtils.getContainer();

    @Mock
    private BasicDataSource dataSource;

    @BeforeAll
    static void setUpAll() throws Exception {
        mysql.start();
        BasicDataSource DATASOURCE = DatabaseUtil.createLocalDataSource(mysql.getJdbcUrl());
        DBUtils.runMigrations(DATASOURCE);
    }

    @BeforeEach
    void setUp() {
        hostAgentDAO = new DBHostAgentDAOImpl(dataSource);
    }

    @Test
    void testGetMainEnvIdbyHostId() throws SQLException {
        // GIVEN
        String hostId = "host123";
        EnvironBean expectedEnvBean = new EnvironBean();
        expectedEnvBean.setId("env123");
        expectedEnvBean.setName("test-env");

        ResultSetHandler<EnvironBean> resultSetHandler = new BeanHandler<>(EnvironBean.class);
        when(dataSource.query(eq(DBHostAgentDAOImpl.GET_MAIN_ENV_BY_HOSTID), eq(resultSetHandler), eq(hostId)))
                .thenReturn(expectedEnvBean);

        // WHEN
        EnvironBean actualEnvBean = hostAgentDAO.getMainEnvIdbyHostId(hostId);

        // THEN
        assertEquals(expectedEnvBean, actualEnvBean);
    }
}