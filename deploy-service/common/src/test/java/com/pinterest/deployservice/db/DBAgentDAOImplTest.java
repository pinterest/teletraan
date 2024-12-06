package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.HostAgentBean;
import com.pinterest.deployservice.bean.KnoxStatus;
import com.pinterest.deployservice.bean.NormandieStatus;
import com.pinterest.deployservice.dao.HostAgentDAO;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DBAgentDAOImplTest {

    private static BasicDataSource dataSource;
    private static HostAgentDAO hostAgentDAO;

    @BeforeAll
    public static void setUpClass() throws Exception {
        dataSource = DBUtils.createTestDataSource();

        hostAgentDAO = new DBHostAgentDAOImpl(dataSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        DBUtils.truncateAllTables(dataSource);
    }


    @Test
    public void testHostAgentDAO() throws Exception {
        final String hostId = "host-1";

        // Test Insert and getById
        HostAgentBean hostAgentBean = genDefaultHostAgentBean(hostId);
        hostAgentDAO.insert(hostAgentBean);
        HostAgentBean getByIdBean = hostAgentDAO.getHostById(hostId);
        assertEquals(hostAgentBean, getByIdBean);

        // Test Update and getById
        hostAgentBean.setIp("192.168.0.1");
        hostAgentDAO.update(hostId, hostAgentBean);
        HostAgentBean getByIdBean2 = hostAgentDAO.getHostById(hostId);
        assertEquals(hostAgentBean, getByIdBean2);

        // Test getHostByName
        HostAgentBean getByNameBean = hostAgentDAO.getHostByName(hostAgentBean.getHost_name());
        assertEquals(hostAgentBean, getByNameBean);

        // Test getDistinctHostsCount
        long hostCount = hostAgentDAO.getDistinctHostsCount();
        assertEquals(1, hostCount);

        // Test getStaleHosts
        List<HostAgentBean> staleHosts = hostAgentDAO.getStaleHosts(System.currentTimeMillis() - 100_000);
        assertTrue(staleHosts.isEmpty());

        List<HostAgentBean> staleHosts2 = hostAgentDAO.getStaleHosts(System.currentTimeMillis() + 100_000);
        assertEquals(1, staleHosts2.size());
        assertEquals(hostAgentBean, staleHosts2.get(0));

        List<HostAgentBean> staleHosts3 = hostAgentDAO.getStaleHosts(
            System.currentTimeMillis() - 100_000, System.currentTimeMillis() + 100_000);
        assertEquals(1, staleHosts3.size());
        assertEquals(hostAgentBean, staleHosts3.get(0));

        // Test Delete
        hostAgentDAO.delete(hostId);
        HostAgentBean getByIdBean3 = hostAgentDAO.getHostById(hostId);
        assertNull(getByIdBean3);
        long hostCount2 = hostAgentDAO.getDistinctHostsCount();
        assertEquals(0, hostCount2);
    }

    private HostAgentBean genDefaultHostAgentBean(String hostId) {
        return HostAgentBean.builder()
            .ip("127.0.0.1")
            .host_id(hostId)
            .host_name(UUID.randomUUID().toString())
            .create_date(System.currentTimeMillis())
            .last_update(System.currentTimeMillis())
            .agent_version("1.0")
            .auto_scaling_group("auto-scaling-group")
            .normandie_status(NormandieStatus.OK)
            .knox_status(KnoxStatus.OK)
            .build();
    }

}
