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
package com.pinterest.deployservice;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pinterest.arcee.autoscaling.AlarmManager;
import com.pinterest.arcee.autoscaling.AutoScaleGroupManager;
import com.pinterest.arcee.aws.AwsConfigManager;
import com.pinterest.arcee.dao.*;
import com.pinterest.arcee.metrics.MetricSource;
import com.pinterest.deployservice.chat.ChatManager;
import com.pinterest.deployservice.dao.*;
import com.pinterest.deployservice.email.MailManager;
import com.pinterest.deployservice.events.EventSender;
import com.pinterest.deployservice.group.HostGroupManager;
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
    private HostInfoDAO hostInfoDAO;
    private GroupDAO groupDAO;
    private GroupInfoDAO groupInfoDAO;
    private AlarmManager alarmManager;
    private HostGroupManager hostGroupDAO;
    private UserRolesDAO userRolesDAO;
    private GroupRolesDAO groupRolesDAO;
    private TokenRolesDAO tokenRolesDAO;
    private ImageDAO imageDAO;
    private HealthCheckDAO healthCheckDAO;
    private HealthCheckErrorDAO healthCheckErrorDAO;
    private NewInstanceReportDAO newInstanceReportDAO;
    private AsgLifecycleEventDAO asgLifecycleEventDAO;

    private AutoScaleGroupManager autoScaleGroupManager;
    private String serviceStage;
    private MailManager mailManager;
    private SourceControlManager sourceControlManager;
    private ChatManager chatManager;
    private ExecutorService jobPool;
    private AmazonEC2Client ec2Client;
    private ConfigHistoryDAO configHistoryDAO;
    private AWSCredentials awsCredentials;
    private MetricSource metricSource;
    AwsConfigManager awsConfigManager;

    private boolean buildCacheEnabled;
    private String buildCacheSpec;
    private String deployCacheSpec;
    private boolean deployCacheEnabled;
    private String deployBoardUrlPrefix;
    private String changeFeedUrl;

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

    public void setHostInfoDAO(HostInfoDAO hostInfoDAO) {
        this.hostInfoDAO = hostInfoDAO;
    }

    public HostInfoDAO getHostInfoDAO() {
        return this.hostInfoDAO;
    }

    public void setImageDAO(ImageDAO imageDAO) {
        this.imageDAO = imageDAO;
    }

    public ImageDAO getImageDAO() {
        return imageDAO;
    }

    public void setHealthCheckDAO(HealthCheckDAO healthCheckDAO) {
        this.healthCheckDAO = healthCheckDAO;
    }

    public HealthCheckDAO getHealthCheckDAO() {
        return healthCheckDAO;
    }

    public void setHealthCheckErrorDAO(HealthCheckErrorDAO healthCheckErrorDAO) {
        this.healthCheckErrorDAO = healthCheckErrorDAO;
    }

    public HealthCheckErrorDAO getHealthCheckErrorDAO() {
        return healthCheckErrorDAO;
    }

    public void setnewInstanceReportDAO(NewInstanceReportDAO newInstanceReportDAO) {
        this.newInstanceReportDAO = newInstanceReportDAO;
    }

    public NewInstanceReportDAO getNewInstanceReportDAO() {
        return newInstanceReportDAO;
    }

    public void setAsgLifecycleEventDAO(AsgLifecycleEventDAO asgLifecycleEventDAO) {
        this.asgLifecycleEventDAO = asgLifecycleEventDAO;
    }

    public AsgLifecycleEventDAO getAsgLifecycleEventDAO() {
        return asgLifecycleEventDAO;
    }

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

    public void setSourceControlManager(SourceControlManager sourceControlManager) {
        this.sourceControlManager = sourceControlManager;
    }

    public AutoScaleGroupManager getAutoScaleGroupManager() {
        return autoScaleGroupManager;
    }

    public AlarmManager getAlarmManager() {
        return alarmManager;
    }

    public void setAlarmManager(AlarmManager alarmManager) {
        this.alarmManager = alarmManager;
    }

    public SourceControlManager getSourceControlManager() {
        return sourceControlManager;
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

    public void setConfigHistoryDAO(ConfigHistoryDAO configHistoryDAO) {
        this.configHistoryDAO = configHistoryDAO;
    }

    public ConfigHistoryDAO getConfigHistoryDAO() {
        return configHistoryDAO;
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


    public void setAutoScaleGroupManager(AutoScaleGroupManager autoScaleGroupManager) {
        this.autoScaleGroupManager = autoScaleGroupManager;
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

    public AwsConfigManager getAwsConfigManager() {
        return awsConfigManager;
    }

    public void setAwsConfigManager(AwsConfigManager awsConfigManager) {
        this.awsConfigManager = awsConfigManager;
    }

    public MetricSource getMetricSource() { return metricSource; }

    public void setMetricSource(MetricSource metricSource) {
        this.metricSource = metricSource;
    }
}
