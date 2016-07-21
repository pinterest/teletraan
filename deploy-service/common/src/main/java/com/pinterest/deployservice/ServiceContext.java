/*
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
package com.pinterest.deployservice;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pinterest.arcee.autoscaling.AlarmManager;
import com.pinterest.arcee.autoscaling.AutoScalingManager;
import com.pinterest.arcee.aws.AwsConfigManager;
import com.pinterest.arcee.dao.*;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.chat.ChatManager;
import com.pinterest.deployservice.dao.*;
import com.pinterest.deployservice.email.MailManager;
import com.pinterest.deployservice.events.EventSender;
import com.pinterest.deployservice.group.HostGroupManager;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.scm.SourceControlManager;

import org.apache.commons.dbcp.BasicDataSource;

import java.util.concurrent.ExecutorService;

public class ServiceContext {
    private BasicDataSource dataSource;
    private BuildDAO buildDAO;
    private AgentDAO agentDAO;
    private AgentErrorDAO agentErrorDAO;
    private AlarmDAO alarmDAO;
    private DeployDAO deployDAO;
    private EnvironDAO environDAO;
    private HostDAO hostDAO;
    private HotfixDAO hotfixDAO;
    private DataDAO dataDAO;
    private UtilDAO utilDAO;
    private RatingDAO ratingDAO;
    private EventSender eventSender;
    private PromoteDAO promoteDAO;
    private GroupDAO groupDAO;
    private GroupInfoDAO groupInfoDAO;
    private AlarmManager alarmManager;
    private HostGroupManager hostGroupDAO;
    private UserRolesDAO userRolesDAO;
    private GroupRolesDAO groupRolesDAO;
    private TokenRolesDAO tokenRolesDAO;
    private ConfigHistoryDAO configHistoryDAO;
    private SpotAutoScalingDAO spotAutoScalingDAO;
    private TagDAO tagDAO;

    private String serviceStage;
    private MailManager mailManager;
    private SourceControlManager sourceControlManager;
    private ChatManager chatManager;
    private ExecutorService jobPool;
    private AmazonEC2Client ec2Client;
    private AWSCredentials awsCredentials;
    private AutoScalingManager autoScalingManager;
    private MetricSource metricSource;
    private AwsConfigManager awsConfigManager;
    private RodimusManager rodimusManager;

    private boolean buildCacheEnabled;
    private String buildCacheSpec;
    private String deployCacheSpec;
    private boolean deployCacheEnabled;
    private String deployBoardUrlPrefix;
    private String changeFeedUrl;

    private ReservedInstanceInfoDAO reservedInstanceInfoDAO;
    private ManagingGroupDAO managingGroupDAO;

    private String quboleAuthentication;
    private int spotAutoScalingThreshold;

    public GroupRolesDAO getGroupRolesDAO() {
        return groupRolesDAO;
    }

    public void setGroupRolesDAO(GroupRolesDAO groupRolesDAO) {
        this.groupRolesDAO = groupRolesDAO;
    }

    public BuildDAO getBuildDAO() {
        return buildDAO;
    }

    public void setBuildDAO(BuildDAO buildDAO) {
        this.buildDAO = buildDAO;
    }

    public AgentDAO getAgentDAO() {
        return agentDAO;
    }

    public void setAgentDAO(AgentDAO agentDAO) {
        this.agentDAO = agentDAO;
    }

    public AgentErrorDAO getAgentErrorDAO() {
        return agentErrorDAO;
    }

    public void setAgentErrorDAO(AgentErrorDAO agentErrorDAO) {
        this.agentErrorDAO = agentErrorDAO;
    }

    public AlarmDAO getAlarmDAO() {
        return alarmDAO;
    }

    public void setAlarmDAO(AlarmDAO alarmDAO) {
        this.alarmDAO = alarmDAO;
    }

    public DeployDAO getDeployDAO() {
        return deployDAO;
    }

    public void setDeployDAO(DeployDAO deployDAO) {
        this.deployDAO = deployDAO;
    }

    public EnvironDAO getEnvironDAO() {
        return environDAO;
    }

    public void setEnvironDAO(EnvironDAO environDAO) {
        this.environDAO = environDAO;
    }

    public HotfixDAO getHotfixDAO() {
        return hotfixDAO;
    }

    public void setHotfixDAO(HotfixDAO hotfixDAO) {
        this.hotfixDAO = hotfixDAO;
    }

    public MailManager getMailManager() {
        return mailManager;
    }

    public void setMailManager(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    public DataDAO getDataDAO() {
        return dataDAO;
    }

    public void setDataDAO(DataDAO dataDAO) {
        this.dataDAO = dataDAO;
    }

    public HostDAO getHostDAO() {
        return hostDAO;
    }

    public void setHostDAO(HostDAO hostDAO) {
        this.hostDAO = hostDAO;
    }

    public UtilDAO getUtilDAO() {
        return utilDAO;
    }

    public void setUtilDAO(UtilDAO utilDAO) {
        this.utilDAO = utilDAO;
    }

    public void setDataSource(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }

    public PromoteDAO getPromoteDAO() {
        return promoteDAO;
    }

    public void setPromoteDAO(PromoteDAO promoteDAO) {
        this.promoteDAO = promoteDAO;
    }

    public GroupDAO getGroupDAO() {
        return groupDAO;
    }

    public void setGroupDAO(GroupDAO groupDAO) {
        this.groupDAO = groupDAO;
    }

    public void setGroupInfoDAO(GroupInfoDAO groupInfoDAO) {
        this.groupInfoDAO = groupInfoDAO;
    }

    public GroupInfoDAO getGroupInfoDAO() {
        return groupInfoDAO;
    }

    public void setHostGroupDAO(HostGroupManager hostGroupDAO) {
        this.hostGroupDAO = hostGroupDAO;
    }

    public HostGroupManager getHostGroupDAO() {
        return hostGroupDAO;
    }

    public ReservedInstanceInfoDAO getReservedInstanceInfoDAO() {
        return reservedInstanceInfoDAO;
    }

    public void setReservedInstanceInfoDAO(ReservedInstanceInfoDAO reservedInstanceInfoDAO) {
        this.reservedInstanceInfoDAO = reservedInstanceInfoDAO;
    }

    public void setConfigHistoryDAO(ConfigHistoryDAO configHistoryDAO) {
        this.configHistoryDAO = configHistoryDAO;
    }

    public ConfigHistoryDAO getConfigHistoryDAO() {
        return configHistoryDAO;
    }

    public SpotAutoScalingDAO getSpotAutoScalingDAO() { return spotAutoScalingDAO; }

    public void setSpotAutoScalingDAO(SpotAutoScalingDAO spotAutoScalingDAO) { this.spotAutoScalingDAO = spotAutoScalingDAO; }

    public void setEventSender(EventSender sender) {
        this.eventSender = sender;
    }

    public EventSender getEventSender() {
        return this.eventSender;
    }

    public void setServiceStage(String serviceStage) {
        this.serviceStage = serviceStage;
    }

    public String getServiceStage() {
        return this.serviceStage;
    }

    public SourceControlManager getSourceControlManager() {
        return sourceControlManager;
    }

    public void setSourceControlManager(SourceControlManager sourceControlManager) {
        this.sourceControlManager = sourceControlManager;
    }

    public AutoScalingManager getAutoScalingManager() {
        return autoScalingManager;
    }

    public void setAutoScalingManager(AutoScalingManager autoScalingManager) {
        this.autoScalingManager = autoScalingManager;
    }

    public AlarmManager getAlarmManager() {
        return alarmManager;
    }

    public void setAlarmManager(AlarmManager alarmManager) {
        this.alarmManager = alarmManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public void setChatManager(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public ExecutorService getJobPool() {
        return jobPool;
    }

    public void setJobPool(ExecutorService jobPool) {
        this.jobPool = jobPool;
    }

    public RatingDAO getRatingDAO() {
        return ratingDAO;
    }

    public void setRatingDAO(RatingDAO ratingDAO) {
        this.ratingDAO = ratingDAO;
    }

    public AmazonEC2Client getEc2Client() {
        return ec2Client;
    }

    public void setEc2Client(AmazonEC2Client client) {
        ec2Client = client;
    }

    public UserRolesDAO getUserRolesDAO() {
        return userRolesDAO;
    }

    public void setUserRolesDAO(UserRolesDAO userRolesDAO) {
        this.userRolesDAO = userRolesDAO;
    }

    public TokenRolesDAO getTokenRolesDAO() {
        return tokenRolesDAO;
    }

    public void setTokenRolesDAO(TokenRolesDAO tokenRolesDAO) {
        this.tokenRolesDAO = tokenRolesDAO;
    }

    public AWSCredentials getAwsCredentials() {
        return awsCredentials;
    }

    public AwsConfigManager getAwsConfigManager() {
        return awsConfigManager;
    }

    public void setAwsConfigManager(AwsConfigManager awsConfigManager) {
        this.awsConfigManager = awsConfigManager;
    }

    public RodimusManager getRodimusManager() {
        return rodimusManager;
    }

    public void setRodimusManager(RodimusManager rodimusManager) {
        this.rodimusManager = rodimusManager;
    }

    public MetricSource getMetricSource() {
        return metricSource;
    }

    public void setMetricSource(MetricSource metricSource) {
        this.metricSource = metricSource;
    }

    public void setBuildCacheEnabled(boolean buildCacheEnabled) {
        this.buildCacheEnabled = buildCacheEnabled;
    }

    public void setBuildCacheSpec(String buildCacheSpec) {
        this.buildCacheSpec = buildCacheSpec;
    }

    public void setDeployCacheSpec(String deployCacheSpec) {
        this.deployCacheSpec = deployCacheSpec;
    }

    public void setDeployCacheEnabled(boolean deployCacheEnabled) {
        this.deployCacheEnabled = deployCacheEnabled;
    }

    public void setDeployBoardUrlPrefix(String deployBoardUrlPrefix) {
        this.deployBoardUrlPrefix = deployBoardUrlPrefix;
    }

    public boolean isBuildCacheEnabled() {
        return buildCacheEnabled;
    }

    public String getBuildCacheSpec() {
        return buildCacheSpec;
    }

    public boolean isDeployCacheEnabled() {
        return deployCacheEnabled;
    }

    public String getDeployCacheSpec() {
        return deployCacheSpec;
    }

    public String getDeployBoardUrlPrefix() {
        return deployBoardUrlPrefix;
    }

    public String getChangeFeedUrl() {
        return changeFeedUrl;
    }

    public void setChangeFeedUrl(String changeFeedUrl) {
        this.changeFeedUrl = changeFeedUrl;
    }

    public void setManagingGroupDAO(ManagingGroupDAO managingGroupDAO) {
        this.managingGroupDAO = managingGroupDAO;
    }

    public ManagingGroupDAO getManagingGroupDAO() {
        return managingGroupDAO;
    }

    public void setQuboleAuthentication(String quboleAuthentication) {
        this.quboleAuthentication = quboleAuthentication;
    }

    public String getQuboleAuthentication() {
        return quboleAuthentication;
    }

    public void setSpotAutoScalingThreshold(int spotAutoScalingThreshold) {
        this.spotAutoScalingThreshold = spotAutoScalingThreshold;
    }

    public int getSpotAutoScalingThreshold() { return spotAutoScalingThreshold; }

    public TagDAO getTagDAO() {
        return tagDAO;
    }

    public void setTagDAO(TagDAO tagDAO) {
        this.tagDAO = tagDAO;
    }
}
