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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
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

import org.apache.commons.codec.binary.Base64;
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
        groupInfoDAO.insertGroupInfo(groupBean);
        SpotAutoScalingBean spotAutoScalingBean = new SpotAutoScalingBean();
        spotAutoScalingBean.setAsg_name("test2-spot");
        spotAutoScalingBean.setBid_price("0.6");
        spotAutoScalingBean.setCluster_name("test2");
        spotAutoScalingBean.setLaunch_config_id("l-2");
        spotAutoScalingBean.setSpot_ratio(0.5);
        spotAutoScalingDAO.insertAutoScalingGroupToCluster("test2-spot", spotAutoScalingBean);

        // no launch config update, only update group table
        GroupBean updatedBean2 = new GroupBean();
        updatedBean2.setGroup_name("test2");
        updatedBean2.setChatroom("hello");
        groupHandler.updateLaunchConfig("test2", updatedBean2);

        // verify
        GroupBean expectedResultBean = generateDefaultBean("test2", "ami-12345", "test2", "test2-1", lastUpdate);
        expectedResultBean.setChatroom("hello");
        ArgumentCaptor<AwsVmBean> argument = ArgumentCaptor.forClass(AwsVmBean.class);
        verify(mockAwsManager, never()).createLaunchConfig(eq("test2"), argument.capture());

    }

    @Test
    public void updateLaunchConfigTest() throws Exception {
        // update launch config, no asg updates
        Long lastUpdate = System.currentTimeMillis();

        GroupBean updatedBean = generateDefaultBean("test3", "ami-12345", "test2", null, lastUpdate);
        groupInfoDAO.insertGroupInfo(updatedBean);

        GroupBean updatedBean2 = new GroupBean();
        updatedBean2.setAssign_public_ip(Boolean.TRUE);
        updatedBean2.setGroup_name("test3");

        groupHandler.updateLaunchConfig("test3", updatedBean2);
        ArgumentCaptor<AwsVmBean> argument = ArgumentCaptor.forClass(AwsVmBean.class);
        verify(mockAwsManager, times(1)).createLaunchConfig(eq("test3"), argument.capture());
        AwsVmBean actualUpdatedBean = argument.getValue();

        assertTrue(actualUpdatedBean.getAssignPublicIp());
        assertEquals(actualUpdatedBean.getRole(), "base");
        assertEquals(actualUpdatedBean.getImage(), "ami-12345");
        assertEquals(actualUpdatedBean.getHostType(), "c3.2xlarge");
        assertEquals(argument.getValue().getRawUserDataString(), Base64.encodeBase64String("#cloud-config\nrole: test3\n".getBytes()));
    }

    @Test
    public void updateSubnetConfigChange() throws Exception {
        // no launch config update, asg updates
        Long lastUpdat = System.currentTimeMillis();
        GroupBean groupBean = generateDefaultBean("test4", "ami-12345", "subnet-1", "config-1", lastUpdat);
        groupInfoDAO.insertGroupInfo(groupBean);

        GroupBean updatedBean = new GroupBean();
        updatedBean.setGroup_name("test4");
        updatedBean.setUser_data("#cloud-config\nrole: test4\n");
        updatedBean.setSubnets("subnet-2");
        updatedBean.setImage_id("ami-12345");

        when(mockAwsManager.getAutoScalingGroupStatus("test4")).thenReturn(ASGStatus.ENABLED);
        groupHandler.updateLaunchConfig("test4", updatedBean);
        ArgumentCaptor<AwsVmBean> argument = ArgumentCaptor.forClass(AwsVmBean.class);
        ArgumentCaptor<AwsVmBean> argument2 = ArgumentCaptor.forClass(AwsVmBean.class);
        verify(mockAwsManager, never()).createLaunchConfig(eq("test4"), argument.capture());
        verify(mockAwsManager, times(1)).updateAutoScalingGroup(eq("test4"), argument2.capture());
        AwsVmBean updateBean =  argument2.getValue();
        assertNull(updateBean.getLaunchConfigId());
        assertNull(updateBean.getTerminationPolicy());
        assertNull(updateBean.getMinSize());
        assertEquals(updateBean.getSubnet(), "subnet-2");

    }


    @Test
    public void updateAMITest() throws Exception {
        Long lastUpdate = System.currentTimeMillis();
        GroupBean groupBean1 = generateDefaultBean("group6", "ami-12345", "subnet-6", "config-6", lastUpdate);
        groupInfoDAO.insertGroupInfo(groupBean1);

        when(mockAwsManager.createLaunchConfig(eq("group6"), any())).thenReturn("config-7");
        when(mockAwsManager.getAutoScalingGroupStatus(any())).thenReturn(ASGStatus.ENABLED);
        groupHandler.updateImageId(groupBean1, "ami-123457");

        ArgumentCaptor<AwsVmBean> argument = ArgumentCaptor.forClass(AwsVmBean.class);
        verify(mockAwsManager, times(1)).createLaunchConfig(eq("group6"), argument.capture());
        AwsVmBean b = argument.getValue();
        assertEquals(b.getImage(), "ami-123457");
        assertEquals(b.getHostType(), groupBean1.getInstance_type());
        assertEquals(b.getSecurityZone(), groupBean1.getSecurity_group());
        assertEquals(b.getAssignPublicIp(), groupBean1.getAssign_public_ip());
        assertEquals(b.getRole(), groupBean1.getIam_role());
        assertEquals(b.getRawUserDataString(), groupBean1.getUser_data());

        GroupBean resultGroupBean = groupInfoDAO.getGroupInfo("group6");
        assertEquals(resultGroupBean.getGroup_name(), "group6");
        assertEquals(resultGroupBean.getAsg_status(), groupBean1.getAsg_status());
        assertEquals(resultGroupBean.getIam_role(), groupBean1.getIam_role());
        assertEquals(resultGroupBean.getLaunch_config_id(), "config-7");
        assertEquals(resultGroupBean.getImage_id(), "ami-123457");
        assertEquals(resultGroupBean.getUser_data(), groupBean1.getUser_data());
        assertEquals(resultGroupBean.getInstance_type(), groupBean1.getInstance_type());
    }

    private GroupBean generateDefaultBean(String groupName, String ami_id, String subnets, String config, Long lastUpdate) {
        GroupBean groupBean = new GroupBean();
        groupBean.setAsg_status(ASGStatus.UNKNOWN);
        groupBean.setAssign_public_ip(false);
        groupBean.setGroup_name(groupName);
        groupBean.setIam_role("base");
        groupBean.setInstance_type("c3.2xlarge");
        groupBean.setSecurity_group("s-test");
        groupBean.setImage_id(ami_id);
        groupBean.setSubnets(subnets);
        groupBean.setLaunch_config_id(config);
        groupBean.setLaunch_latency_th(600);
        groupBean.setLast_update(lastUpdate);
        groupBean.setUser_data(Base64.encodeBase64String(String.format("#cloud-config\nrole: %s\n", groupName).getBytes()));
        groupBean.setHealthcheck_state(false);
        groupBean.setLifecycle_state(false);
        return groupBean;
    }
}
