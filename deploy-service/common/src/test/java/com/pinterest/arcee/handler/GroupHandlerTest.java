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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.arcee.autoscaling.AwsAlarmManager;
import com.pinterest.arcee.autoscaling.AwsAutoScaleGroupManager;
import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.arcee.bean.ImageBean;
import com.pinterest.arcee.dao.AlarmDAO;
import com.pinterest.arcee.dao.GroupInfoDAO;
import com.pinterest.arcee.dao.ImageDAO;
import com.pinterest.arcee.db.DBAlarmDAOImpl;
import com.pinterest.arcee.db.DBGroupInfoDAOImpl;
import com.pinterest.arcee.db.DBImageDAOImpl;
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
    private static AwsAutoScaleGroupManager mockAwsManager;
    private static AwsAlarmManager mockAlarmWatcher;
    private static CMDBHostGroupManager mockCMDBDAO;
    private static GroupHandler groupHandler;
    private static ImageDAO imageDAO;

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

        mockCMDBDAO = mock(CMDBHostGroupManager.class);
        mockAwsManager = mock(AwsAutoScaleGroupManager.class);
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
        context.setAutoScaleGroupManager(mockAwsManager);
        context.setAlarmManager(mockAlarmWatcher);
        context.setImageDAO(imageDAO);
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
    public void createGroupTest() throws Exception {
        GroupBean groupBean = new GroupBean();
        groupBean.setGroup_name("test1");
        groupBean.setSubnets("test2");
        Long lastUpdate = System.currentTimeMillis();
        groupBean.setLast_update(lastUpdate);
        GroupBean resultGroupBean = generateDefaultBean("test1", "ami-12345", "test2", null, lastUpdate);
        when(mockAwsManager.createLaunchConfig(resultGroupBean)).thenReturn("test1-1");
        groupHandler.createGroup("test1", groupBean);

        ArgumentCaptor<GroupBean> argument = ArgumentCaptor.forClass(GroupBean.class);
        verify(mockAwsManager, times(1)).createLaunchConfig(argument.capture());

        GroupBean resultGroupBean2 = generateDefaultBean("test1", "ami-12345", "test2", "test1-1", lastUpdate);
        GroupBean b = argument.getValue();
        assertEquals(b.getGroup_name(), resultGroupBean2.getGroup_name());

        assertEquals(b.getIam_role(), resultGroupBean2.getIam_role());
        assertEquals(b.getGroup_name(), resultGroupBean2.getGroup_name());
        assertEquals(b.getInstance_type(), resultGroupBean2.getInstance_type());
        assertEquals(b.getUser_data(), resultGroupBean2.getUser_data());

        GroupBean groupBean1 = groupInfoDAO.getGroupInfo("test1");
        assertEquals(groupBean1.getIam_role(), resultGroupBean2.getIam_role());
        assertEquals(groupBean1.getGroup_name(), resultGroupBean2.getGroup_name());
        assertEquals(groupBean1.getInstance_type(), resultGroupBean2.getInstance_type());
        assertEquals(groupBean1.getUser_data(), resultGroupBean2.getUser_data());
    }

    @Test
    public void updateGroupBeanTest() throws Exception {
        Long lastUpdate = System.currentTimeMillis();

        GroupBean groupBean = generateDefaultBean("test2", "ami-12345", "test2", "test2-1", lastUpdate);
        groupInfoDAO.insertGroupInfo(groupBean);

        // no launch config update, only update group table
        GroupBean updatedBean2 = new GroupBean();
        updatedBean2.setGroup_name("test2");
        updatedBean2.setChatroom("hello");
        groupHandler.updateLaunchConfig("test2", updatedBean2);

        // verfiy
        GroupBean expectedResultBean = generateDefaultBean("test2", "ami-12345", "test2", "test2-1", lastUpdate);
        expectedResultBean.setChatroom("hello");
        verify(mockAwsManager, never()).createLaunchConfig(expectedResultBean);

        GroupBean actualResultBean = groupInfoDAO.getGroupInfo("test2");
        assertEquals(actualResultBean.getChatroom(), expectedResultBean.getChatroom());
        assertEquals(actualResultBean.getUser_data(), groupBean.getUser_data());

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
        ArgumentCaptor<GroupBean> argument = ArgumentCaptor.forClass(GroupBean.class);
        verify(mockAwsManager, times(1)).createLaunchConfig(argument.capture());
        GroupBean actualUpdatedBean = argument.getValue();
        assertEquals(actualUpdatedBean.getGroup_name(), "test3");
        assertEquals(actualUpdatedBean.getAsg_status(), ASGStatus.UNKNOWN);
        assertTrue(actualUpdatedBean.getAssign_public_ip());
        assertEquals(actualUpdatedBean.getIam_role(), "base");
        assertEquals(actualUpdatedBean.getImage_id(), "ami-12345");
        assertEquals(actualUpdatedBean.getInstance_type(), "c3.2xlarge");
        assertEquals(argument.getValue().getUser_data(), Base64.encodeBase64String("#cloud-config\nrole: test3\n".getBytes()));
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

        when(mockAwsManager.hasAutoScalingGroup("test4")).thenReturn(Boolean.TRUE);
        groupHandler.updateLaunchConfig("test4", updatedBean);
        ArgumentCaptor<GroupBean> argument = ArgumentCaptor.forClass(GroupBean.class);
        verify(mockAwsManager, never()).createLaunchConfig(argument.capture());
        verify(mockAwsManager, times(1)).updateSubnet("test4", "subnet-2");
    }

    @Test
    public void updateBothConfigAndSubnetChangeTest() throws Exception {
        // update launch config, asg updates
        Long lastUpdate = System.currentTimeMillis();
        GroupBean groupBean = generateDefaultBean("test5", "ami-12345", "subnet-3", "config-2", lastUpdate);
        groupInfoDAO.insertGroupInfo(groupBean);

        GroupBean updatedBean = new GroupBean();
        updatedBean.setGroup_name("test5");
        updatedBean.setImage_id("ami-123456");
        updatedBean.setSubnets("subnet-4");
        when(mockAwsManager.hasAutoScalingGroup("test5")).thenReturn(Boolean.TRUE);
        groupHandler.updateLaunchConfig("test5", updatedBean);
        ArgumentCaptor<GroupBean> argument = ArgumentCaptor.forClass(GroupBean.class);
        verify(mockAwsManager, times(1)).createLaunchConfig(argument.capture());
        GroupBean actualUpdatedBean = argument.getValue();

        assertEquals(actualUpdatedBean.getGroup_name(), "test5");
        assertFalse(actualUpdatedBean.getAssign_public_ip());
        assertEquals(actualUpdatedBean.getIam_role(), "base");
        assertEquals(actualUpdatedBean.getImage_id(), "ami-123456");
        assertEquals(actualUpdatedBean.getUser_data(), Base64.encodeBase64String("#cloud-config\nrole: test5\n".getBytes()));

        verify(mockAwsManager, times(1)).updateSubnet("test5", "subnet-4");

        GroupBean resultGroupBean = groupInfoDAO.getGroupInfo("test5");
        assertEquals(resultGroupBean.getGroup_name(), "test5");
        assertFalse(resultGroupBean.getAssign_public_ip());
        assertEquals(resultGroupBean.getIam_role(), "base");
        assertEquals(resultGroupBean.getImage_id(), "ami-123456");
        assertEquals(resultGroupBean.getSubnets(), "subnet-4");
        assertEquals(resultGroupBean.getUser_data(), Base64.encodeBase64String("#cloud-config\nrole: test5\n".getBytes()));
    }

    @Test
    public void updateAMITest() throws Exception {
        Long lastUpdate = System.currentTimeMillis();
        GroupBean groupBean1 = generateDefaultBean("group6", "ami-12345", "subnet-6", "config-6", lastUpdate);
        groupInfoDAO.insertGroupInfo(groupBean1);

        when(mockAwsManager.createLaunchConfig(any())).thenReturn("config-7");
        when(mockAwsManager.hasAutoScalingGroup(any())).thenReturn(Boolean.TRUE);
        groupHandler.updateImageId(groupBean1, "ami-123457");

        ArgumentCaptor<GroupBean> argument = ArgumentCaptor.forClass(GroupBean.class);
        verify(mockAwsManager, times(1)).createLaunchConfig(argument.capture());
        GroupBean b = argument.getValue();
        assertEquals(b.getImage_id(), "ami-123457");
        assertEquals(b.getInstance_type(), groupBean1.getInstance_type());
        assertEquals(b.getSecurity_group(), groupBean1.getSecurity_group());
        assertEquals(b.getAssign_public_ip(), groupBean1.getAssign_public_ip());
        assertEquals(b.getIam_role(), groupBean1.getIam_role());
        assertEquals(b.getUser_data(), groupBean1.getUser_data());

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
        groupBean.setSecurity_group("sg-5fb76336");
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
