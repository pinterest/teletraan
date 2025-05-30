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
package com.pinterest.deployservice.handler;

import com.google.common.base.Joiner;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.allowlists.Allowlist;
import com.pinterest.deployservice.bean.AcceptanceStatus;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.BuildTagBean;
import com.pinterest.deployservice.bean.CommitBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.DeployFilterBean;
import com.pinterest.deployservice.bean.DeployQueryResultBean;
import com.pinterest.deployservice.bean.DeployState;
import com.pinterest.deployservice.bean.DeployType;
import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.EnvWebHookBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.bean.PromoteDisablePolicy;
import com.pinterest.deployservice.bean.PromoteType;
import com.pinterest.deployservice.bean.ScheduleBean;
import com.pinterest.deployservice.bean.ScheduleState;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.bean.UpdateStatement;
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.buildtags.BuildTagsManagerImpl;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.common.StateMachines;
import com.pinterest.deployservice.common.WebhookDataFactory;
import com.pinterest.deployservice.dao.AgentDAO;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.PromoteDAO;
import com.pinterest.deployservice.dao.ScheduleDAO;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.db.DatabaseUtil;
import com.pinterest.deployservice.db.DeployQueryFilter;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.universal.http.HttpClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Interval;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployHandler implements DeployHandlerInterface {
    private static final Logger LOG = LoggerFactory.getLogger(DeployHandler.class);
    private static final String FEED_TEMPLATE =
            "{\"type\":\"Deploy\","
                    + "\"environment\":\"%s (%s)\","
                    + "\"url\":\"http://deploy.pinadmin.com/deploy/%s\","
                    + "\"description\":\"Deploy %s\","
                    + "\"author\":\"%s\","
                    + "\"automation\":\"%s\","
                    + "\"source\":\"Teletraan\","
                    + "\"nimbus_uuid\":\"%s\"}";
    private static final String COMPARE_DEPLOY_URL =
            "https://deploy.pinadmin.com/env/%s/%s/compare_deploys_2/?chkbox_1=%s&chkbox_2=%s";

    static final String ERROR_EMPTY_BUILD_ID = "Build id can not be empty.";
    static final String ERROR_BUILD_NAME_NOT_MATCH_STAGE_CONFIG =
            "Build name (%s) does not match stage config (%s).";
    static final String ERROR_NON_PRIVATE_UNTRUSTED_LOCATION =
            "Non-private build url points to an untrusted location (%s)."
                    + " Please Contact #teletraan to ensure the build artifact is published to a trusted url.";
    static final String ERROR_STAGE_NOT_ALLOW_PRIVATE_BUILD =
            "This stage does not allow deploying a private build. "
                    + "Please Contact #teletraan to allow your stage for deploying private build.";
    static final String ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_STAGE =
            "This stage requires SOX builds. A private build cannot be used in a sox-compliant stage.";
    static final String ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_SOURCE =
            "This stage requires SOX builds. The build must be from a sox-compliant source. Contact your sox administrators.";
    private static final HttpClient httpClient = HttpClient.builder().build();

    private final DeployDAO deployDAO;
    private final EnvironDAO environDAO;
    private final BuildDAO buildDAO;
    private final PromoteDAO promoteDAO;
    private final AgentDAO agentDAO;
    private final ScheduleDAO scheduleDAO;
    private final TagDAO tagDAO;
    private final BasicDataSource dataSource;
    private final CommonHandler commonHandler;
    private final DataHandler dataHandler;
    private final SourceControlManagerProxy sourceControlManagerProxy;
    private final ExecutorService jobPool;
    private final String deployBoardUrlPrefix;
    private final String changeFeedUrl;
    private final BuildTagsManager buildTagsManager;
    private final Allowlist buildAllowlist;
    private final ConfigHistoryHandler configHistoryHandler;
    static final String PRIVATE_BUILD_SCM_BRANCH = "private";

    private final class NotifyJob implements Callable<Void> {
        private final EnvironBean envBean;
        private final DeployBean newDeployBean;
        private final DeployBean oldDeployBean;
        private final CommonHandler commonHandler;
        private final String deployBoardUrlPrefix;
        private final String changeFeedUrl;

        public NotifyJob(
                EnvironBean envBean,
                DeployBean newDeployBean,
                DeployBean oldDeployBean,
                CommonHandler commonHandler,
                String deployBoardUrlPrefix,
                String changeFeedUrl) {
            this.envBean = envBean;
            this.newDeployBean = newDeployBean;
            this.oldDeployBean = oldDeployBean;
            this.commonHandler = commonHandler;
            this.deployBoardUrlPrefix = deployBoardUrlPrefix;
            this.changeFeedUrl = changeFeedUrl;
        }

        private void updateChangeFeed() {
            try {
                String autoPromote = "False";
                if (newDeployBean.getOperator().equals(Constants.AUTO_PROMOTER_NAME))
                    autoPromote = "True";
                String feedPayload =
                        String.format(
                                FEED_TEMPLATE,
                                envBean.getEnv_name(),
                                envBean.getStage_name(),
                                newDeployBean.getDeploy_id(),
                                newDeployBean.getDeploy_type(),
                                newDeployBean.getOperator(),
                                autoPromote,
                                envBean.getExternal_id());
                httpClient.post(changeFeedUrl, feedPayload, null);
                LOG.info("Send change feed {} to {} successfully", feedPayload, changeFeedUrl);
            } catch (Exception e) {
                LOG.error(
                        "Failed to send deploy info to Change Feed for deploy {}",
                        newDeployBean.getDeploy_id());
            }
        }

        void sendStartDeployMessage(String additionalMessage) {
            try {
                String operator = newDeployBean.getOperator();
                String buildId = newDeployBean.getBuild_id();
                DeployType deployType = newDeployBean.getDeploy_type();
                BuildBean buildBean = buildDAO.getById(buildId);
                String WebLink =
                        deployBoardUrlPrefix
                                + String.format(
                                        "/env/%s/%s/deploy/",
                                        envBean.getEnv_name(), envBean.getStage_name());

                String action = commonHandler.getDeployAction(deployType);
                String message =
                        String.format(
                                "%s/%s: %s %s/%s started. See details %s.",
                                envBean.getEnv_name(),
                                envBean.getStage_name(),
                                action,
                                buildBean.getScm_branch(),
                                buildBean.getScm_commit_7(),
                                WebLink);

                commonHandler.sendChatMessage(
                        operator,
                        envBean.getChatroom(),
                        message,
                        "yellow",
                        envBean.getGroup_mention_recipients());
                if (StringUtils.isNotEmpty(additionalMessage)) {
                    LOG.debug(
                            String.format(
                                    "Sending additional message: %s to chat", additionalMessage));
                    commonHandler.sendChatMessage(
                            operator,
                            envBean.getChatroom(),
                            additionalMessage,
                            "yellow",
                            envBean.getGroup_mention_recipients());
                }
            } catch (Exception e) {
                LOG.error("Failed to send start notification!", e);
            }
        }

        public Void call() {
            String additionalMessage = generateMentions(envBean, newDeployBean, oldDeployBean);
            sendStartDeployMessage(additionalMessage);
            LOG.info(
                    "Successfully send deploy start message for deploy {}",
                    newDeployBean.getDeploy_id());

            if (!StringUtils.isEmpty(changeFeedUrl)) {
                updateChangeFeed();
                LOG.info(
                        "Successfully send deploy info to Change Feed for deploy {}",
                        newDeployBean.getDeploy_id());
            }

            return null;
        }
    }

    public DeployHandler(ServiceContext serviceContext) {
        deployDAO = serviceContext.getDeployDAO();
        environDAO = serviceContext.getEnvironDAO();
        buildDAO = serviceContext.getBuildDAO();
        promoteDAO = serviceContext.getPromoteDAO();
        agentDAO = serviceContext.getAgentDAO();
        scheduleDAO = serviceContext.getScheduleDAO();
        tagDAO = serviceContext.getTagDAO();
        dataSource = serviceContext.getDataSource();
        commonHandler = new CommonHandler(serviceContext);
        dataHandler = new DataHandler(serviceContext);
        sourceControlManagerProxy = serviceContext.getSourceControlManagerProxy();
        jobPool = serviceContext.getJobPool();
        deployBoardUrlPrefix = serviceContext.getDeployBoardUrlPrefix();
        changeFeedUrl = serviceContext.getChangeFeedUrl();
        buildTagsManager = new BuildTagsManagerImpl(tagDAO);
        buildAllowlist = serviceContext.getBuildAllowlist();
        configHistoryHandler = new ConfigHistoryHandler(serviceContext);
    }

    private String generateMentions(
            EnvironBean envBean, DeployBean newDeployBean, DeployBean oldDeployBean) {
        DeployType deployType = newDeployBean.getDeploy_type();
        if (deployType == DeployType.RESTART) {
            // It does not make sense to mentioning author for rollback and restart
            return null;
        }

        if (!envBean.getNotify_authors()) {
            return null;
        }

        String newBuildId = newDeployBean.getBuild_id();
        String oldBuildId = oldDeployBean.getBuild_id();
        if (deployType == DeployType.ROLLBACK) {
            // Reverse the old and new for rollbacks
            newBuildId = oldDeployBean.getBuild_id();
            oldBuildId = newDeployBean.getBuild_id();
        }

        try {
            BuildBean newBuildBean = buildDAO.getById(newBuildId);
            BuildBean oldBuildBean = buildDAO.getById(oldBuildId);

            // Get all the commits between old and new deploy
            if (!newBuildBean.getScm().equals(oldBuildBean.getScm())) {
                LOG.error(
                        "Failed to generate author notification message: new and old commit has different SCM type");
                return null;
            }

            List<CommitBean> commits =
                    sourceControlManagerProxy.getCommits(
                            newBuildBean.getScm(),
                            newBuildBean.getScm_repo(),
                            newBuildBean.getScm_commit(),
                            oldBuildBean.getScm_commit(),
                            0);

            Set<String> authors = new HashSet<>();
            for (CommitBean commit : commits) {
                if (commit.getAuthor().equalsIgnoreCase("unknown")) {
                    // ignore unknown authors
                    continue;
                }
                // TODO hipchat is different, screw it for now
                authors.add(String.format("<@%s|%s>", commit.getAuthor(), commit.getAuthor()));
            }

            String mentions = Joiner.on(",").join(authors);
            String compareUrl =
                    String.format(
                            COMPARE_DEPLOY_URL,
                            envBean.getEnv_name(),
                            envBean.getStage_name(),
                            newBuildId,
                            oldBuildId);

            if (deployType.equals(DeployType.ROLLBACK)) {
                return String.format(
                        "This rollback reverses %d commits from the following pingineers: %s. See changes <%s|here>",
                        commits.size(), mentions, compareUrl);
            } else {
                return String.format(
                        "This deploy features %d commits from the following pingineers: %s. See changes <%s|here>",
                        commits.size(), mentions, compareUrl);
            }
        } catch (Exception e) {
            LOG.error("Failed to generate author notification message", e);
            return null;
        }
    }

    String internalDeploy(EnvironBean envBean, DeployBean deployBean) throws Exception {
        // TODO deploy becomes longer process, consider to have worker to do this step
        String deployId = CommonUtils.getBase64UUID();
        deployBean.setDeploy_id(deployId);
        deployBean.setAcc_status(Constants.DEFAULT_ACCEPTANCE_STATUS);
        long now = System.currentTimeMillis();
        deployBean.setLast_update(now);

        deployBean.setState(DeployState.RUNNING);
        deployBean.setStart_date(now);
        long total = agentDAO.countAgentByEnv(envBean.getEnv_id());
        deployBean.setSuc_total(0);
        deployBean.setFail_total(0);
        deployBean.setTotal((int) total);

        // Do a transactional update for everything need to
        List<UpdateStatement> statements = new ArrayList<>();
        statements.add(deployDAO.genInsertStatement(deployBean));

        EnvironBean updateEnvBean = new EnvironBean();
        updateEnvBean.setDeploy_id(deployId);
        updateEnvBean.setDeploy_type(deployBean.getDeploy_type());
        updateEnvBean.setStage_type(envBean.getStage_type());

        statements.add(environDAO.genUpdateStatement(envBean.getEnv_id(), updateEnvBean));

        // Deprecate/Obsolete the previous deploy
        DeployBean oldDeployBean = null;
        String oldDeployId = envBean.getDeploy_id();
        DeployState finalState = null;
        if (oldDeployId != null) {
            oldDeployBean = deployDAO.getById(oldDeployId);

            finalState = StateMachines.FINAL_STATE_TRANSITION_MAP.get(oldDeployBean.getState());
            DeployBean updatedDeployBean = new DeployBean();
            updatedDeployBean.setState(finalState);

            if (!StateMachines.FINAL_ACCEPTANCE_STATUSES.contains(oldDeployBean.getAcc_status())) {
                updatedDeployBean.setAcc_status(AcceptanceStatus.TERMINATED);
            }

            updatedDeployBean.setLast_update(System.currentTimeMillis());
            statements.add(deployDAO.genUpdateStatement(oldDeployId, updatedDeployBean));
        }

        LOG.debug("Create and persist deploy {} ", deployBean);
        DatabaseUtil.transactionalUpdate(dataSource, statements);
        LOG.info(
                "Announce new deploy {} for env {} and retire older deploy {} to state {}",
                deployId,
                envBean.getEnv_id(),
                oldDeployId,
                finalState);

        jobPool.submit(
                new NotifyJob(
                        envBean,
                        deployBean,
                        oldDeployBean,
                        commonHandler,
                        deployBoardUrlPrefix,
                        changeFeedUrl));

        // Submit pre-deploy Webhook, if exists
        EnvWebHookBean webhook =
                dataHandler.getDataById(envBean.getWebhooks_config_id(), WebhookDataFactory.class);
        if (webhook != null && !CollectionUtils.isEmpty(webhook.getPreDeployHooks())) {
            jobPool.submit(new WebhookJob(webhook.getPreDeployHooks(), deployBean));
        }

        LOG.info("Submitted notify job for deploy {}", deployId);

        return deployId;
    }

    public DeployBean getDeploySafely(String deployId) throws Exception {
        if (deployId == null) {
            throw new DeployInternalException("No deploy exists yet.");
        }
        DeployBean deployBean = deployDAO.getById(deployId);
        if (deployBean == null) {
            throw new DeployInternalException("Deploy %s does not exist.", deployId);
        }
        return deployBean;
    }

    // Disable the Auto Promote if this is user behavior, and DisablePolicy agrees
    public void disableAutoPromote(EnvironBean envBean, String operator, boolean force) {
        try {
            if (!force && operator.equals(Constants.AUTO_PROMOTER_NAME)) {
                // Do not disable if this is auto deploy
                return;
            }

            PromoteBean promoteBean = promoteDAO.getById(envBean.getEnv_id());
            if (promoteBean == null || promoteBean.getType() == PromoteType.MANUAL) {
                // No need to update
                return;
            }

            if (!force && promoteBean.getDisable_policy() == PromoteDisablePolicy.MANUAL) {
                // Disable policy does not allow disable ( has to manually do it )
                return;
            }

            // Otherwise, disable auto promote
            PromoteBean updateBean = new PromoteBean();
            updateBean.setType(PromoteType.MANUAL);
            updateBean.setLast_operator(operator);
            updateBean.setLast_update(System.currentTimeMillis());
            promoteDAO.update(envBean.getEnv_id(), updateBean);
            configHistoryHandler.updateConfigHistory(
                    envBean.getEnv_id(), Constants.TYPE_ENV_PROMOTE, updateBean, "auto-deploy");
            LOG.info(
                    "Disable auto promote for env {} due to the manual deploy by {}",
                    envBean,
                    operator);
        } catch (Exception e) {
            LOG.error("Failed to disable auto promote env {}", envBean, e);
        }
    }

    /*
      Validate the env and build id requirements:
        1. the build_id is not null
        2. build name must match the build name configured in env
        3. non-private deploy from a trusted url
        4. private build deploy from allow_private_build env
        5. no private build for sox env
        6. only sox build can be deployed for sox env
    */
    protected void validateBuild(EnvironBean envBean, String buildId) throws Exception {
        if (StringUtils.isEmpty(buildId)) {
            throw new WebApplicationException(ERROR_EMPTY_BUILD_ID, Response.Status.BAD_REQUEST);
        }
        BuildBean buildBean = buildDAO.getById(buildId);

        // check build name must match stage config
        if (!buildBean.getBuild_name().equals(envBean.getBuild_name())) {
            throw new DeployInternalException(
                    ERROR_BUILD_NAME_NOT_MATCH_STAGE_CONFIG,
                    buildBean.getBuild_name(),
                    envBean.getBuild_name());
        }

        if (envBean.getStage_type() != EnvType.DEV
                && buildAllowlist != null
                && !buildAllowlist.trusted(buildBean.getArtifact_url())
                && isPrivateBuild(buildBean)) {
            buildBean.setScm_branch(PRIVATE_BUILD_SCM_BRANCH);
        }

        // only allow a non-private deploy if the build is from a trusted artifact url
        if (envBean.getEnsure_trusted_build()
                && !isPrivateBuild(buildBean)
                && buildAllowlist != null
                && !buildAllowlist.trusted(buildBean.getArtifact_url())) {
            throw new WebApplicationException(
                    String.format(
                            ERROR_NON_PRIVATE_UNTRUSTED_LOCATION, buildBean.getArtifact_url()),
                    Response.Status.BAD_REQUEST);
        }
        // if the stage is not allowed (allow_private_build)
        if (!envBean.getAllow_private_build()) {
            // only allow deploy if it is not private build
            if (isPrivateBuild(buildBean)) {
                throw new WebApplicationException(
                        ERROR_STAGE_NOT_ALLOW_PRIVATE_BUILD, Response.Status.BAD_REQUEST);
            }
        }
        // disallow sox deploy if the build artifact is private
        if (envBean.getIs_sox() && isPrivateBuild(buildBean)) {
            throw new WebApplicationException(
                    ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_STAGE, Response.Status.BAD_REQUEST);
        }
        // disallow sox deploy if the build artifact is not from a sox source url
        if (envBean.getIs_sox()
                && buildAllowlist != null
                && !buildAllowlist.sox_compliant(buildBean.getArtifact_url())) {
            throw new WebApplicationException(
                    ERROR_STAGE_REQUIRES_SOX_BUILD_COMPLIANT_SOURCE, Response.Status.BAD_REQUEST);
        }
    }

    private boolean isPrivateBuild(BuildBean buildBean) {
        return buildBean.getScm_branch() == null
                || buildBean.getScm_branch().toLowerCase().startsWith(PRIVATE_BUILD_SCM_BRANCH);
    }

    public String deploy(
            EnvironBean envBean, String buildId, String desc, String deliveryType, String operator)
            throws Exception {
        validateBuild(envBean, buildId);

        DeployBean deployBean = new DeployBean();
        deployBean.setEnv_id(envBean.getEnv_id());
        deployBean.setBuild_id(buildId);
        deployBean.setDescription(desc);
        deployBean.setDeploy_type(DeployType.REGULAR);
        deployBean.setOperator(operator);

        // compare the current stage_type and the delivery_type
        // When the delivery_type is different with stage_type and stage_type is not DEFAULT, we log
        // the deployment API call;
        // When the delivery_type is different with stage_type and stage_type is DEFAULT, we update
        // the stage_type;
        if (StringUtils.isNotBlank(deliveryType)
                && !envBean.getStage_type().toString().equals(deliveryType)) {
            if (envBean.getStage_type() != EnvType.DEFAULT) {
                String errorMessage =
                        String.format(
                                "The delivery type %s is different with the stage type %s for %s/%s",
                                deliveryType,
                                envBean.getStage_type(),
                                envBean.getEnv_name(),
                                envBean.getStage_name());
                LOG.error(errorMessage);
                throw new WebApplicationException(errorMessage, Response.Status.CONFLICT);
            } else {
                EnvType type = EnvType.valueOf(deliveryType);
                envBean.setStage_type(type);
                LOG.info(
                        "The stage type is updated from {} to {} for env {}",
                        envBean.getStage_type(),
                        type,
                        envBean.getEnv_id());
            }
        }

        disableAutoPromote(envBean, operator, false);
        resetSchedule(envBean);

        return internalDeploy(envBean, deployBean);
    }

    public void update(String deployId, DeployBean updateBean) throws Exception {
        updateBean.setLast_update(System.currentTimeMillis());
        // TODO use oldStatus to do atomic update status or state
        deployDAO.update(deployId, updateBean);
    }

    public String promote(
            EnvironBean envBean, String fromDeployId, String description, String operator)
            throws Exception {
        // TODO: Should get by id and type ?
        TagBean tagBean = tagDAO.getLatestByTargetId(envBean.getEnv_id());
        if (tagBean != null && tagBean.getValue() == TagValue.DISABLE_ENV) {
            throw new DeployInternalException(
                    String.format(
                            "Can not promote to a disabled env %s/%s",
                            envBean.getEnv_name(), envBean.getStage_name()));
        }

        DeployBean fromDeployBean = getDeploySafely(fromDeployId);

        validateBuild(envBean, fromDeployBean.getBuild_id());

        DeployBean deployBean = new DeployBean();
        deployBean.setEnv_id(envBean.getEnv_id());
        deployBean.setBuild_id(fromDeployBean.getBuild_id());
        deployBean.setDescription(description);
        deployBean.setDeploy_type(DeployType.REGULAR);
        deployBean.setOperator(operator);
        deployBean.setFrom_deploy(fromDeployId);

        disableAutoPromote(envBean, operator, false);

        return internalDeploy(envBean, deployBean);
    }

    DeployBean getLastSucceededDeploy(EnvironBean envBean) throws Exception {
        int index = 1;
        int size = 100;
        DeployFilterBean filterBean = new DeployFilterBean();
        filterBean.setEnvIds(Collections.singletonList(envBean.getEnv_id()));
        filterBean.setPageIndex(index);
        filterBean.setPageSize(size);
        int maxPages = 50; // This makes us check at most 5000 deploys
        int toCheckPages = maxPages;
        while (toCheckPages-- > 0) {
            DeployQueryFilter filter = new DeployQueryFilter(filterBean);
            DeployQueryResultBean resultBean = deployDAO.getAllDeploys(filter);
            if (resultBean.getTotal() < 1) {
                LOG.warn(
                        "Could not find any previous succeeded deploy in env {}",
                        envBean.getEnv_id());
                return null;
            }
            for (DeployBean deploy : resultBean.getDeploys()) {
                if (deploy.getState() == DeployState.SUCCEEDED) {
                    return deploy;
                }
            }
            index += 1;
            filterBean.setPageIndex(index);
        }
        LOG.warn(
                "Latest {} deploys are all failed for {}. Give up",
                size * maxPages,
                envBean.getEnv_id());

        return null;
    }

    public String rollback(
            EnvironBean envBean, String toDeployId, String description, String operator)
            throws Exception {
        DeployBean toDeployBean;
        if (toDeployId == null) {
            toDeployBean = getLastSucceededDeploy(envBean);
            if (toDeployBean == null) {
                throw new DeployInternalException(
                        String.format(
                                "Could not find last succeeded deploy for env %s/%s",
                                envBean.getEnv_name(), envBean.getStage_name()));
            }
        } else {
            toDeployBean = getDeploySafely(toDeployId);
        }

        DeployBean deployBean = new DeployBean();
        deployBean.setEnv_id(envBean.getEnv_id());
        deployBean.setBuild_id(toDeployBean.getBuild_id());
        deployBean.setDescription(description);
        deployBean.setDeploy_type(DeployType.ROLLBACK);
        deployBean.setOperator(operator);
        deployBean.setAlias(toDeployId);

        disableAutoPromote(envBean, operator, false);

        return internalDeploy(envBean, deployBean);
    }

    public String restart(EnvironBean envBean, String description, String operator)
            throws Exception {
        DeployBean prevDeployBean = getDeploySafely(envBean.getDeploy_id());
        DeployBean deployBean = new DeployBean();
        deployBean.setEnv_id(envBean.getEnv_id());
        deployBean.setBuild_id(prevDeployBean.getBuild_id());
        deployBean.setDescription(description);
        deployBean.setDeploy_type(DeployType.RESTART);
        deployBean.setOperator(operator);

        disableAutoPromote(envBean, operator, false);
        resetSchedule(envBean);
        return internalDeploy(envBean, deployBean);
    }

    public void resetSchedule(EnvironBean envBean) throws Exception {
        String scheduleId = envBean.getSchedule_id();
        if (scheduleId != null) {
            ScheduleBean updateScheduleBean = new ScheduleBean();
            updateScheduleBean.setId(scheduleId);
            updateScheduleBean.setState(ScheduleState.RUNNING);
            updateScheduleBean.setCurrent_session(1);
            updateScheduleBean.setState_start_time(System.currentTimeMillis());
            scheduleDAO.update(updateScheduleBean, scheduleId);
        }
    }

    public List<DeployBean> getDeployCandidates(
            String envId, Interval interval, int size, boolean onlyGoodBuilds) throws Exception {
        LOG.info(
                "Search Deploy candidates between {} and {} for environment {}",
                interval.getStart().toString(ISODateTimeFormat.dateTime()),
                interval.getEnd().toString(ISODateTimeFormat.dateTime()),
                envId);
        List<DeployBean> taggedGoodDeploys = new ArrayList<>();

        List<DeployBean> availableDeploys = deployDAO.getAcceptedDeploys(envId, interval, size);

        if (!onlyGoodBuilds) {
            return availableDeploys;
        }

        if (!availableDeploys.isEmpty()) {
            Map<String, DeployBean> buildId2DeployBean = new HashMap<>();
            for (DeployBean deployBean : availableDeploys) {
                String buildId = deployBean.getBuild_id();
                if (StringUtils.isNotEmpty(buildId)) {
                    buildId2DeployBean.put(buildId, deployBean);
                }
            }
            List<BuildBean> availableBuilds =
                    buildDAO.getBuildsFromIds(buildId2DeployBean.keySet());
            List<BuildTagBean> buildTagBeanList =
                    buildTagsManager.getEffectiveTagsWithBuilds(availableBuilds);
            for (BuildTagBean buildTagBean : buildTagBeanList) {
                if (buildTagBean.getTag() != null
                        && buildTagBean.getTag().getValue() == TagValue.BAD_BUILD) {
                    // bad build,  do not include
                    LOG.info(
                            "Env {} Build {} is tagged as BAD_BUILD, ignore",
                            envId,
                            buildTagBean.getBuild());
                } else {
                    String buildId = buildTagBean.getBuild().getBuild_id();
                    taggedGoodDeploys.add(buildId2DeployBean.get(buildId));
                }
            }
        }
        // should order deploy bean by start date desc
        if (taggedGoodDeploys.size() > 0) {
            taggedGoodDeploys.sort(
                    (d1, d2) -> Long.compare(d2.getStart_date(), d1.getStart_date()));
            LOG.info(
                    "Env {} the first deploy candidate is {}",
                    envId,
                    taggedGoodDeploys.get(0).getBuild_id());
        }
        return taggedGoodDeploys;
    }
}
