/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

import com.pinterest.deployservice.allowlists.Allowlist;
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.chat.ChatManager;
import com.pinterest.deployservice.ci.Buildkite;
import com.pinterest.deployservice.ci.CIPlatformManagerProxy;
import com.pinterest.deployservice.ci.Jenkins;
import com.pinterest.deployservice.dao.AgentCountDAO;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.AgentErrorDAO;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.ConfigHistoryDAO;
import com.pinterest.deployservice.dao.DataDAO;
import com.pinterest.deployservice.dao.DeployConstraintDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostTagDAO;
import com.pinterest.deployservice.dao.HotfixDAO;
import com.pinterest.deployservice.dao.PindeployDAO;
import com.pinterest.deployservice.dao.PromoteDAO;
import com.pinterest.deployservice.dao.RatingDAO;
import com.pinterest.deployservice.dao.ScheduleDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.deployservice.dao.UtilDAO;
import com.pinterest.deployservice.email.MailManager;
import com.pinterest.deployservice.events.BuildEventPublisher;
import com.pinterest.deployservice.pingrequests.PingRequestValidator;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.universal.events.AppEventPublisher;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.apache.commons.dbcp.BasicDataSource;

public class ServiceContext {
    private BasicDataSource dataSource;
    private BuildDAO buildDAO;
    private AgentDAO agentDAO;
    private AgentCountDAO agentCountDAO;
    private AgentErrorDAO agentErrorDAO;
    private DeployDAO deployDAO;
    private EnvironDAO environDAO;
    private HostDAO hostDAO;
    private HostAgentDAO hostAgentDAO;
    private HotfixDAO hotfixDAO;
    private DataDAO dataDAO;
    private UtilDAO utilDAO;
    private RatingDAO ratingDAO;
    private PromoteDAO promoteDAO;
    private GroupDAO groupDAO;
    private UserRolesDAO userRolesDAO;
    private GroupRolesDAO groupRolesDAO;
    private TokenRolesDAO tokenRolesDAO;
    private ConfigHistoryDAO configHistoryDAO;
    private TagDAO tagDAO;
    private ScheduleDAO scheduleDAO;
    private HostTagDAO hostTagDAO;
    private DeployConstraintDAO deployConstraintDAO;
    private PindeployDAO pindeployDAO;

    private Allowlist buildAllowlist;

    private String serviceStage;
    private MailManager mailManager;
    private SourceControlManagerProxy sourceControlManagerProxy;
    private CIPlatformManagerProxy ciPlatformManagerProxy;
    private ChatManager chatManager;
    private ExecutorService jobPool;
    private RodimusManager rodimusManager;
    private BuildTagsManager buildTagsManager;

    private boolean buildCacheEnabled;
    private String buildCacheSpec;
    private String deployCacheSpec;
    private boolean deployCacheEnabled;
    private String deployBoardUrlPrefix;
    private String changeFeedUrl;
    private Jenkins jenkins;
    private Buildkite buildkite;
    private List<PingRequestValidator> pingRequestValidators;
    private Long agentCountCacheTtl;
    private Long maxParallelThreshold;
    private BuildEventPublisher buildEventPublisher;
    private Set<String> accountAllowList;

    // Publishers & Listeners
    private AppEventPublisher appEventPublisher;

    public AppEventPublisher getAppEventPublisher() {
        return appEventPublisher;
    }

    public void setAppEventPublisher(AppEventPublisher appEventPublisher) {
        this.appEventPublisher = appEventPublisher;
    }

    public Allowlist getBuildAllowlist() {
        return buildAllowlist;
    }

    public void setBuildAllowlist(Allowlist buildAllowlist) {
        this.buildAllowlist = buildAllowlist;
    }

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

    public AgentCountDAO getAgentCountDAO() {
        return agentCountDAO;
    }

    public void setAgentCountDAO(AgentCountDAO agentCountDAO) {
        this.agentCountDAO = agentCountDAO;
    }

    public AgentErrorDAO getAgentErrorDAO() {
        return agentErrorDAO;
    }

    public void setAgentErrorDAO(AgentErrorDAO agentErrorDAO) {
        this.agentErrorDAO = agentErrorDAO;
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

    public PindeployDAO getPindeployDAO() {
        return pindeployDAO;
    }

    public void setPindeployDAO(PindeployDAO pindeployDAO) {
        this.pindeployDAO = pindeployDAO;
    }

    public HostAgentDAO getHostAgentDAO() {
        return hostAgentDAO;
    }

    public void setHostAgentDAO(HostAgentDAO hostAgentDAO) {
        this.hostAgentDAO = hostAgentDAO;
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

    public HostTagDAO getHostTagDAO() {
        return hostTagDAO;
    }

    public void setHostTagDAO(HostTagDAO hostTagDAO) {
        this.hostTagDAO = hostTagDAO;
    }

    public DeployConstraintDAO getDeployConstraintDAO() {
        return deployConstraintDAO;
    }

    public void setDeployConstraintDAO(DeployConstraintDAO deployConstraintDAO) {
        this.deployConstraintDAO = deployConstraintDAO;
    }

    public void setConfigHistoryDAO(ConfigHistoryDAO configHistoryDAO) {
        this.configHistoryDAO = configHistoryDAO;
    }

    public ConfigHistoryDAO getConfigHistoryDAO() {
        return configHistoryDAO;
    }

    public void setServiceStage(String serviceStage) {
        this.serviceStage = serviceStage;
    }

    public String getServiceStage() {
        return this.serviceStage;
    }

    public SourceControlManagerProxy getSourceControlManagerProxy() {
        return sourceControlManagerProxy;
    }

    public void setSourceControlManagerProxy(SourceControlManagerProxy sourceControlManagerProxy) {
        this.sourceControlManagerProxy = sourceControlManagerProxy;
    }

    public CIPlatformManagerProxy getCIPlatformManagerProxy() {
        return ciPlatformManagerProxy;
    }

    public void setCIPlatformManagerProxy(CIPlatformManagerProxy ciPlatformManagerProxy) {
        this.ciPlatformManagerProxy = ciPlatformManagerProxy;
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

    public RodimusManager getRodimusManager() {
        return rodimusManager;
    }

    public void setRodimusManager(RodimusManager rodimusManager) {
        this.rodimusManager = rodimusManager;
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

    // keep these getter and setter for Jenkins for backward compatibility purpose
    // the new way is to use CIPlatformManagerProxy
    public Jenkins getJenkins() {
        return jenkins;
    }

    public void setJenkins(Jenkins jenkins) {
        this.jenkins = jenkins;
    }

    public Buildkite getBuildkite() {
        return buildkite;
    }

    public void setBuildkite(Buildkite buildkite) {
        this.buildkite = buildkite;
    }

    public TagDAO getTagDAO() {
        return tagDAO;
    }

    public void setTagDAO(TagDAO tagDAO) {
        this.tagDAO = tagDAO;
    }

    public ScheduleDAO getScheduleDAO() {
        return scheduleDAO;
    }

    public void setScheduleDAO(ScheduleDAO scheduleDAO) {
        this.scheduleDAO = scheduleDAO;
    }

    public BuildTagsManager getBuildTagsManager() {
        return buildTagsManager;
    }

    public void setBuildTagsManager(BuildTagsManager buildTagsManager) {
        this.buildTagsManager = buildTagsManager;
    }

    public List<PingRequestValidator> getPingRequestValidators() {
        return pingRequestValidators;
    }

    public void setPingRequestValidators(List<PingRequestValidator> pingRequestValidators) {
        this.pingRequestValidators = pingRequestValidators;
    }

    public Long getAgentCountCacheTtl() {
        return agentCountCacheTtl;
    }

    public void setAgentCountCacheTtl(Long agentCountCacheTttl) {
        this.agentCountCacheTtl = agentCountCacheTttl;
    }

    public Long getMaxParallelThreshold() {
        return maxParallelThreshold;
    }

    public void setMaxParallelThreshold(Long maxParallelThreshold) {
        this.maxParallelThreshold = maxParallelThreshold;
    }

    public BuildEventPublisher getBuildEventPublisher() {
        return buildEventPublisher;
    }

    public void setBuildEventPublisher(BuildEventPublisher buildEventPublisher) {
        this.buildEventPublisher = buildEventPublisher;
    }

    public Set<String> getAccountAllowList() {
        return accountAllowList;
    }

    public void setAccountAllowList(Collection<String> accountAllowList) {
        this.accountAllowList = new HashSet<String>(accountAllowList);
    }
}
