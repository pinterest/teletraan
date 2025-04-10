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
package com.pinterest.teletraan;

import com.pinterest.deployservice.allowlists.BuildAllowlistImpl;
import com.pinterest.deployservice.buildtags.BuildTagsManagerImpl;
import com.pinterest.deployservice.ci.CIPlatformManager;
import com.pinterest.deployservice.ci.CIPlatformManagerProxy;
import com.pinterest.deployservice.ci.Jenkins;
import com.pinterest.deployservice.db.DBAgentCountDAOImpl;
import com.pinterest.deployservice.db.DBAgentDAOImpl;
import com.pinterest.deployservice.db.DBAgentErrorDAOImpl;
import com.pinterest.deployservice.db.DBBuildDAOImpl;
import com.pinterest.deployservice.db.DBConfigHistoryDAOImpl;
import com.pinterest.deployservice.db.DBDataDAOImpl;
import com.pinterest.deployservice.db.DBDeployConstraintDAOImpl;
import com.pinterest.deployservice.db.DBDeployDAOImpl;
import com.pinterest.deployservice.db.DBEnvironDAOImpl;
import com.pinterest.deployservice.db.DBGroupDAOImpl;
import com.pinterest.deployservice.db.DBGroupRolesDAOImpl;
import com.pinterest.deployservice.db.DBHostAgentDAOImpl;
import com.pinterest.deployservice.db.DBHostDAOImpl;
import com.pinterest.deployservice.db.DBHostTagDAOImpl;
import com.pinterest.deployservice.db.DBHotfixDAOImpl;
import com.pinterest.deployservice.db.DBPindeployDAOImpl;
import com.pinterest.deployservice.db.DBPromoteDAOImpl;
import com.pinterest.deployservice.db.DBRatingsDAOImpl;
import com.pinterest.deployservice.db.DBScheduleDAOImpl;
import com.pinterest.deployservice.db.DBTagDAOImpl;
import com.pinterest.deployservice.db.DBTokenRolesDAOImpl;
import com.pinterest.deployservice.db.DBUserRolesDAOImpl;
import com.pinterest.deployservice.db.DBUtilDAOImpl;
import com.pinterest.deployservice.events.EventBridgePublisher;
import com.pinterest.deployservice.pingrequests.PingRequestValidator;
import com.pinterest.deployservice.rodimus.DefaultRodimusManager;
import com.pinterest.deployservice.rodimus.RodimusManagerImpl;
import com.pinterest.deployservice.scm.SourceControlManager;
import com.pinterest.deployservice.scm.SourceControlManagerProxy;
import com.pinterest.teletraan.config.AppEventFactory;
import com.pinterest.teletraan.config.BuildAllowlistFactory;
import com.pinterest.teletraan.config.JenkinsFactory;
import com.pinterest.teletraan.config.CIPlatformFactory;
import com.pinterest.teletraan.config.RodimusFactory;
import com.pinterest.teletraan.config.SourceControlFactory;
import com.pinterest.teletraan.config.WorkerConfig;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory;
import com.pinterest.teletraan.universal.events.AppEventPublisher;
import com.pinterest.teletraan.worker.AgentJanitor;
import com.pinterest.teletraan.worker.AutoPromoter;
import com.pinterest.teletraan.worker.BuildJanitor;
import com.pinterest.teletraan.worker.DeployJanitor;
import com.pinterest.teletraan.worker.DeployTagWorker;
import com.pinterest.teletraan.worker.HostTerminator;
import com.pinterest.teletraan.worker.HotfixStateTransitioner;
import com.pinterest.teletraan.worker.MetricsEmitter;
import com.pinterest.teletraan.worker.SimpleAgentJanitor;
import com.pinterest.teletraan.worker.StateTransitioner;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigHelper.class);
    private static final int DEFAULT_PERIOD = 30;
    private static final int DEFAULT_MAX_STALE_HOST_THRESHOLD_SECONDS = 600; // 10 min
    private static final int DEFAULT_MIN_STALE_HOST_THRESHOLD_SECONDS = 150; // 2.5 min
    private static final int DEFAULT_LAUNCH_LATENCY_THRESHOLD_SECONDS = 600;
    private static final String DEFAULT_DEPLOY_JANITOR_SCHEDULE = "0 30 3 * * ?";
    private static final String DEFAULT_BUILD_JANITOR_SCHEDULE = "0 40 3 * * ?";
    private static final int DEFAULT_MAX_DAYS_TO_KEEP = 180;
    private static final int DEFAULT_MAX_BUILDS_TO_KEEP = 1000;

    public static TeletraanServiceContext setupContext(
            TeletraanServiceConfiguration configuration, Environment environment) throws Exception {
        TeletraanServiceContext context = new TeletraanServiceContext();

        BasicDataSource dataSource = configuration.getDataSourceFactory().build();
        context.setDataSource(dataSource);

        context.setUserRolesDAO(new DBUserRolesDAOImpl(dataSource));
        context.setGroupRolesDAO(new DBGroupRolesDAOImpl(dataSource));
        context.setTokenRolesDAO(new DBTokenRolesDAOImpl(dataSource));

        context.setBuildDAO(new DBBuildDAOImpl(dataSource));
        context.setEnvironDAO(new DBEnvironDAOImpl(dataSource));
        context.setDeployDAO(new DBDeployDAOImpl(dataSource));
        context.setHotfixDAO(new DBHotfixDAOImpl(dataSource));
        context.setRatingDAO(new DBRatingsDAOImpl(dataSource));
        context.setPromoteDAO(new DBPromoteDAOImpl(dataSource));

        context.setDataDAO(new DBDataDAOImpl(dataSource));
        context.setPindeployDAO(new DBPindeployDAOImpl(dataSource));
        context.setUtilDAO(new DBUtilDAOImpl(dataSource));

        context.setConfigHistoryDAO(new DBConfigHistoryDAOImpl(dataSource));
        context.setHostDAO(new DBHostDAOImpl(dataSource));
        context.setHostAgentDAO(new DBHostAgentDAOImpl(dataSource));
        context.setHostTagDAO(new DBHostTagDAOImpl(dataSource));
        context.setDeployConstraintDAO(new DBDeployConstraintDAOImpl(dataSource));
        context.setGroupDAO(new DBGroupDAOImpl(dataSource));
        context.setAgentDAO(new DBAgentDAOImpl(dataSource));
        context.setAgentCountDAO(new DBAgentCountDAOImpl(dataSource));
        context.setAgentErrorDAO(new DBAgentErrorDAOImpl(dataSource));

        context.setTagDAO(new DBTagDAOImpl(dataSource));
        context.setScheduleDAO(new DBScheduleDAOImpl(dataSource));

        // Inject proper implementation based on config
        context.setAuthorizationFactory(configuration.getAuthorizationFactory());
        context.setAuthZResourceExtractorFactory(
                new TeletraanAuthZResourceExtractorFactory(context));
        context.setChatManager(configuration.getChatFactory().create());
        context.setMailManager(configuration.getEmailFactory().createMailManager());
        context.setBuildTagsManager(new BuildTagsManagerImpl(context.getTagDAO()));

        String defaultScmTypeName = configuration.getDefaultScmTypeName();
        List<SourceControlFactory> sourceControlConfigs = configuration.getSourceControlConfigs();
        Map<String, SourceControlManager> managers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SourceControlFactory scf : sourceControlConfigs) {
            SourceControlManager scm = scf.create();
            String type = scm.getTypeName();
            managers.put(type, scm);
        }
        context.setSourceControlManagerProxy(
                new SourceControlManagerProxy(managers, defaultScmTypeName));

        // CIPlatformFactory replaces JenkinsFactory and BuildkiteFactory
        // This change is backward incompatible as the config file will need to be updated
        // with Jenkins and Buildkite configurations being list items under the "ci" section
        List<CIPlatformFactory> ciPlatformConfigs = configuration.getCIPlatformConfigs();
        if (ciPlatformConfigs != null || !ciPlatformConfigs.isEmpty()) {
            Map<String, CIPlatformManager> ciPlatforms = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (CIPlatformFactory ciPlatformFactory : ciPlatformConfigs) {
                CIPlatformManager ciPlatform = ciPlatformFactory.create();
                String type = ciPlatform.getTypeName();
                ciPlatforms.put(type, ciPlatform);
            }
            context.setCIPlatformManagerProxy(new CIPlatformManagerProxy(ciPlatforms));
        }

        AppEventFactory appEventFactory = configuration.getAppEventFactory();
        if (appEventFactory != null) {
            context.setAppEventPublisher(appEventFactory.createEventPublisher());
        } else {
            context.setAppEventPublisher(new AppEventPublisher() {});
        }

        RodimusFactory rodimusFactory = configuration.getRodimusFactory();
        if (rodimusFactory != null) {
            context.setRodimusManager(
                    new RodimusManagerImpl(
                            rodimusFactory.getRodimusUrl(),
                            rodimusFactory.getKnoxKey(),
                            rodimusFactory.getUseProxy(),
                            rodimusFactory.getHttpProxyAddr(),
                            rodimusFactory.getHttpProxyPort()));
        } else {
            context.setRodimusManager(new DefaultRodimusManager());
        }

        BuildAllowlistFactory buildAllowlistFactory = configuration.getBuildAllowlistFactory();
        if (buildAllowlistFactory != null) {
            context.setBuildAllowlist(
                    new BuildAllowlistImpl(
                            buildAllowlistFactory.getValidBuildURLs(),
                            buildAllowlistFactory.getTrustedBuildURLs(),
                            buildAllowlistFactory.getsoxBuildURLs()));
        } else {
            context.setBuildAllowlist(
                    new BuildAllowlistImpl(
                            new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        }

        JenkinsFactory jenkinsFactory = configuration.getJenkinsFactory();
        if (jenkinsFactory != null) {
            context.setJenkins(
                    new Jenkins(
                            jenkinsFactory.getJenkinsUrl(),
                            jenkinsFactory.getRemoteToken(),
                            jenkinsFactory.getUseProxy(),
                            jenkinsFactory.getHttpProxyAddr(),
                            jenkinsFactory.getHttpProxyPort()));
        }

        LOG.info("External alert factory is {}", configuration.getExternalAlertsConfigs());
        // Set external alerts factory
        if (configuration.getExternalAlertsConfigs() != null) {
            context.setExternalAlertsFactory(
                    configuration.getExternalAlertsConfigs().createExternalAlertFactory());
        }

        if (configuration.getPingRequestValidators() != null) {
            List<PingRequestValidator> validators = new ArrayList<>();
            for (String validator : configuration.getPingRequestValidators()) {
                LOG.info("Add PingRequestValidator {}", validator);
                validators.add((PingRequestValidator) Class.forName(validator).newInstance());
            }
            context.setPingRequestValidators(validators);
        }

        if (configuration.getAwsFactory() != null) {
            context.setBuildEventPublisher(
                    new EventBridgePublisher(
                            configuration.getAwsFactory().buildEventBridgeClient(),
                            configuration.getAwsFactory().getEventBridgeEventBusName()));
        }

        if (configuration.getAccountAllowList() != null) {
            context.setAccountAllowList(configuration.getAccountAllowList());
        }

        int poolSize = Runtime.getRuntime().availableProcessors();
        String jobPoolName = "jobPool";
        ExecutorService jobPool =
                environment
                        .lifecycle()
                        .executorService(jobPoolName)
                        .minThreads(poolSize * 4)
                        .maxThreads(poolSize * 8)
                        .keepAliveTime(Duration.seconds(30))
                        .workQueue(new ArrayBlockingQueue<>(poolSize * 5000, false))
                        .shutdownTime(Duration.seconds(30))
                        .rejectedExecutionHandler(new AbortPolicy())
                        .build();
        new ExecutorServiceMetrics(jobPool, jobPoolName, null).bindTo(Metrics.globalRegistry);
        context.setJobPool(jobPool);

        context.setDeployBoardUrlPrefix(configuration.getSystemFactory().getDashboardUrl());
        context.setChangeFeedUrl(configuration.getSystemFactory().getChangeFeedUrl());

        context.setAclManagementEnabled(configuration.getSystemFactory().isAclManagementEnabled());
        context.setAclManagementDisabledMessage(
                configuration.getSystemFactory().getAclManagementDisabledMessage());

        // Only applies to Teletraan agent service
        context.setAgentCountCacheTtl(configuration.getSystemFactory().getAgentCountCacheTtl());
        context.setMaxParallelThreshold(configuration.getSystemFactory().getMaxParallelThreshold());
        return context;
    }

    public static void scheduleWorkers(
            TeletraanServiceConfiguration configuration, TeletraanServiceContext serviceContext)
            throws Exception {
        List<WorkerConfig> workerConfigs = configuration.getWorkerConfigs();
        for (WorkerConfig config : workerConfigs) {
            String workerName = config.getName();
            Map<String, String> properties = config.getProperties();
            int defaultValue = new Random().nextInt(30);
            int initDelay = MapUtils.getIntValue(properties, "initialDelay", defaultValue);
            int period = MapUtils.getIntValue(properties, "period", DEFAULT_PERIOD);

            if (workerName.equalsIgnoreCase(StateTransitioner.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new StateTransitioner(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled StateTransitioner.");
            }

            if (workerName.equalsIgnoreCase(AutoPromoter.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                int bufferTimeMinutes =
                        MapUtils.getIntValue(
                                properties,
                                "bufferTimeMinutes",
                                AutoPromoter.DEFAULT_BUFFER_TIME_MINUTE);
                Runnable worker =
                        new AutoPromoter(serviceContext).withBufferTimeMinutes(bufferTimeMinutes);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled AutoPromoter.");
            }

            if (workerName.equalsIgnoreCase(HotfixStateTransitioner.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new HotfixStateTransitioner(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled HotfixStateTransitioner.");
            }

            if (workerName.equalsIgnoreCase(SimpleAgentJanitor.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                int minStaleHostThreshold =
                        MapUtils.getIntValue(
                                properties,
                                "minStaleHostThreshold",
                                DEFAULT_MIN_STALE_HOST_THRESHOLD_SECONDS);
                int maxStaleHostThreshold =
                        MapUtils.getIntValue(
                                properties,
                                "maxStaleHostThreshold",
                                DEFAULT_MAX_STALE_HOST_THRESHOLD_SECONDS);
                Runnable worker =
                        new SimpleAgentJanitor(
                                serviceContext, minStaleHostThreshold, maxStaleHostThreshold);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled SimpleAgentJanitor.");
            }

            if (workerName.equalsIgnoreCase(AgentJanitor.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                int minStaleHostThreshold =
                        MapUtils.getIntValue(
                                properties,
                                "minStaleHostThreshold",
                                DEFAULT_MIN_STALE_HOST_THRESHOLD_SECONDS);
                int maxStaleHostThreshold =
                        MapUtils.getIntValue(
                                properties,
                                "maxStaleHostThreshold",
                                DEFAULT_MAX_STALE_HOST_THRESHOLD_SECONDS);
                int maxLaunchLatencyThreshold =
                        MapUtils.getIntValue(
                                properties,
                                "maxLaunchLatencyThreshold",
                                DEFAULT_LAUNCH_LATENCY_THRESHOLD_SECONDS);
                Runnable worker =
                        new AgentJanitor(
                                serviceContext,
                                minStaleHostThreshold,
                                maxStaleHostThreshold,
                                maxLaunchLatencyThreshold);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled AgentJanitor.");
            }

            if (workerName.equalsIgnoreCase(DeployTagWorker.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new DeployTagWorker(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled DeployTagWorker.");
            }

            // Schedule cron like jobs
            JobDetail deployJanitorJob = null;
            CronTrigger deployJanitorTrigger = null;
            if (workerName.equalsIgnoreCase(DeployJanitor.class.getSimpleName())) {
                String schedule =
                        MapUtils.getString(properties, "schedule", DEFAULT_DEPLOY_JANITOR_SCHEDULE);
                deployJanitorJob =
                        JobBuilder.newJob(DeployJanitor.class)
                                .withIdentity("deployJanitorJob", "group1")
                                .build();
                deployJanitorTrigger =
                        TriggerBuilder.newTrigger()
                                .forJob(deployJanitorJob)
                                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                                .build();
            }
            JobDetail buildJanitorJob = null;
            CronTrigger buildJanitorTrigger = null;
            if (workerName.equalsIgnoreCase(BuildJanitor.class.getSimpleName())) {
                String schedule =
                        MapUtils.getString(properties, "schedule", DEFAULT_BUILD_JANITOR_SCHEDULE);
                int maxDaysToKeep =
                        MapUtils.getIntValue(
                                properties, "minStaleHostThreshold", DEFAULT_MAX_DAYS_TO_KEEP);
                int maxBuildsToKeep =
                        MapUtils.getIntValue(
                                properties, "maxStaleHostThreshold", DEFAULT_MAX_BUILDS_TO_KEEP);
                serviceContext.setMaxDaysToKeep(maxDaysToKeep);
                serviceContext.setMaxBuildsToKeep(maxBuildsToKeep);
                buildJanitorJob =
                        JobBuilder.newJob(BuildJanitor.class)
                                .withIdentity("buildJanitorJob", "group1")
                                .build();
                buildJanitorTrigger =
                        TriggerBuilder.newTrigger()
                                .forJob(buildJanitorJob)
                                .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                                .build();
            }

            if (deployJanitorTrigger != null || buildJanitorTrigger != null) {
                Scheduler cronScheduler = new StdSchedulerFactory().getScheduler();
                cronScheduler.getContext().put("serviceContext", serviceContext);
                cronScheduler.start();
                if (deployJanitorTrigger != null) {
                    cronScheduler.scheduleJob(deployJanitorJob, deployJanitorTrigger);
                    LOG.info("Scheduled DeployJanitor.");
                }
                if (buildJanitorTrigger != null) {
                    cronScheduler.scheduleJob(buildJanitorJob, buildJanitorTrigger);
                    LOG.info("Scheduled BuildJanitor.");
                }
            }

            if (workerName.equalsIgnoreCase(HostTerminator.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new HostTerminator(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled HostTerminator.");
            }

            if (workerName.equalsIgnoreCase(MetricsEmitter.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new MetricsEmitter(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled MetricsEmitter.");
            }
        }
    }
}
