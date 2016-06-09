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
package com.pinterest.arcee.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.arcee.autoscaling.AwsAlarmManager;
import com.pinterest.arcee.autoscaling.AwsAutoScalingManager;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.ImageBean;
import com.pinterest.arcee.bean.SpotAutoScalingBean;
import com.pinterest.arcee.dao.AlarmDAO;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.ImageDAO;
import com.pinterest.arcee.dao.SpotAutoScalingDAO;
import com.pinterest.arcee.db.DBAlarmDAOImpl;
import com.pinterest.arcee.db.DBGroupInfoDAOImpl;
import com.pinterest.arcee.db.DBImageDAOImpl;
import com.pinterest.arcee.db.DBSpotAutoScalingDAOImpl;
import com.pinterest.clusterservice.bean.AwsVmBean;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.ASGStatus;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.db.DBGroupDAOImpl;
import com.pinterest.deployservice.db.DBHostDAOImpl;
import com.pinterest.deployservice.db.DBUtilDAOImpl;
import com.pinterest.deployservice.db.DatabaseUtil;
import com.pinterest.deployservice.group.CMDBHostGroupManager;

import com.ibatis.common.jdbc.ScriptRunner;
import com.mysql.management.driverlaunched.ServerLauncherSocketFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;


public class GroupHandlerTest {
    private final static String DEFAULT_BASE_DIR = "/tmp/deploy-unit-test";
    private final static String DEFAULT_DB_NAME = "deploy";
    private final static int DEFAULT_PORT = 3304;

    private static GroupInfoDAO groupInfoDAO;
    private static GroupDAO groupDAO;
    private static HostDAO hostDAO;
    private static AlarmDAO alarmDAO;
    private static ServiceContext context;
    private static UtilDAO utilDAO;
    private static AwsAutoScalingManager mockAwsManager;
    private static AwsAlarmManager mockAlarmWatcher;
    private static CMDBHostGroupManager mockCMDBDAO;
    private static GroupHandler groupHandler;
    private static ImageDAO imageDAO;
    private static SpotAutoScalingDAO spotAutoScalingDAO;

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            // making sure we do not have anything running
            ServerLauncherSocketFactory.shutdown(new File(DEFAULT_BASE_DIR), null);
        } catch (Exception e) {
            // ignore
        }
        BasicDataSource DATASOURCE = DatabaseUtil.createMXJDataSource(DEFAULT_DB_NAME,
                DEFAULT_BASE_DIR, DEFAULT_PORT);
        Connection conn = DATASOURCE.getConnection();
        ScriptRunner runner = new ScriptRunner(conn, false, true);
        runner.runScript(new BufferedReader(new InputStreamReader(
            GroupHandlerTest.class.getResourceAsStream("/sql/cleanup.sql"))));
        runner.runScript(new BufferedReader(new InputStreamReader(
                GroupHandlerTest.class.getResourceAsStream("/sql/deploy.sql"))));
        context = new ServiceContext();
        hostDAO = new DBHostDAOImpl(DATASOURCE);
        groupDAO = new DBGroupDAOImpl(DATASOURCE);
        groupInfoDAO = new DBGroupInfoDAOImpl(DATASOURCE);
        alarmDAO = new DBAlarmDAOImpl(DATASOURCE);
        utilDAO = new DBUtilDAOImpl(DATASOURCE);
        imageDAO = new DBImageDAOImpl(DATASOURCE);
        spotAutoScalingDAO = new DBSpotAutoScalingDAOImpl(DATASOURCE);


        mockCMDBDAO = mock(CMDBHostGroupManager.class);
        mockAwsManager = mock(AwsAutoScalingManager.class);
        mockAlarmWatcher = mock(AwsAlarmManager.class);
        ImageBean imageBean = new ImageBean();
        imageBean.setApp_name("golden_12.04");
        imageBean.setId("ami-12345");
        imageBean.setPublish_date(1L);
        imageDAO.insertOrUpdate(imageBean);

        context.setHostGroupDAO(mockCMDBDAO);
        context.setHostDAO(hostDAO);
        context.setGroupDAO(groupDAO);
        context.setGroupInfoDAO(groupInfoDAO);
        context.setUtilDAO(utilDAO);
        context.setAlarmDAO(alarmDAO);
        context.setAutoScalingManager(mockAwsManager);
        context.setAlarmManager(mockAlarmWatcher);
        context.setImageDAO(imageDAO);
        context.setSpotAutoScalingDAO(spotAutoScalingDAO);
        groupHandler = new GroupHandler(context);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ServerLauncherSocketFactory.shutdown(new File(DEFAULT_BASE_DIR), null);
    }

    @Before
    public void setUp() throws Exception {
        reset(mockAwsManager);
        reset(mockAlarmWatcher);
    }

    @Test
    public void updateGroupBeanTest() throws Exception {
        Long lastUpdate = System.currentTimeMillis();

        GroupBean groupBean = generateDefaultBean("test2", "ami-12345", "test2", "test2-1", lastUpdate);
        groupInfoDAO.insertOrUpdateGroupInfo("test2", groupBean);
        SpotAutoScalingBean spotAutoScalingBean = new SpotAutoScalingBean();
        spotAutoScalingBean.setBid_price("0.6");
        spotAutoScalingBean.setCluster_name("test2");
        spotAutoScalingBean.setSpot_ratio(0.5);
        spotAutoScalingDAO.insertAutoScalingGroupToCluster("test2-spot", spotAutoScalingBean);

        // no launch config update, only update group table
        GroupBean updatedBean2 = new GroupBean();
        updatedBean2.setGroup_name("test2");
        updatedBean2.setChatroom("hello");
        when(mockAwsManager.getAutoScalingGroupStatus("test2")).thenReturn(ASGStatus.UNKNOWN);

        AwsVmBean awsVmBean = generateDefaultAwsVmBean("test2", "ami-12345", "test2", "test2-1");
        when(mockAwsManager.getLaunchConfigInfo("test2-1")).thenReturn(awsVmBean);
        groupHandler.updateGroupInfo("test2", updatedBean2);

        // verify
        GroupBean expectedResultBean = generateDefaultBean("test2", "ami-12345", "test2", "test2-1", lastUpdate);
        expectedResultBean.setChatroom("hello");
        ArgumentCaptor<AwsVmBean> argument = ArgumentCaptor.forClass(AwsVmBean.class);
        verify(mockAwsManager, never()).createLaunchConfig(eq("test2"), argument.capture());

    }

    private GroupBean generateDefaultBean(String groupName, String ami_id, String subnets, String config, Long lastUpdate) {
        GroupBean groupBean = new GroupBean();
        groupBean.setGroup_name(groupName);
        groupBean.setLaunch_latency_th(600);
        groupBean.setLast_update(lastUpdate);
        groupBean.setHealthcheck_state(false);
        groupBean.setLifecycle_state(false);
        return groupBean;
    }

    private AwsVmBean generateDefaultAwsVmBean(String clusterName, String image, String subnets, String configId) {
        AwsVmBean awsVmBean = new AwsVmBean();
        awsVmBean.setAssignPublicIp(false);
        awsVmBean.setRole("base");
        awsVmBean.setHostType("c3.2xlarge");
        awsVmBean.setSecurityZone("s-test");
        awsVmBean.setImage(image);
        awsVmBean.setSubnet(subnets);
        awsVmBean.setLaunchConfigId(configId);
        awsVmBean.setRawUserDataString(String.format("#cloud-config\nrole: %s\n", clusterName));
        return awsVmBean;
    }
}
