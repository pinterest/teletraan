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
package com.pinterest.teletraan;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pinterest.arcee.aws.DefaultHostInfoDAOImpl;
import com.pinterest.arcee.aws.EC2HostInfoDAOImpl;
import com.pinterest.arcee.aws.ReservedInstanceFetcher;
import com.pinterest.arcee.db.*;
import com.pinterest.clusterservice.aws.AwsManagerImpl;
import com.pinterest.clusterservice.db.DBBaseImageDAOImpl;
import com.pinterest.clusterservice.db.DBClusterDAOImpl;
import com.pinterest.clusterservice.db.DBClusterUpgradeEventDAOImpl;
import com.pinterest.clusterservice.db.DBHostTypeDAOImpl;
import com.pinterest.clusterservice.db.DBPlacementDAOImpl;
import com.pinterest.clusterservice.db.DBSecurityZoneDAOImpl;
import com.pinterest.clusterservice.cm.AwsVmManager;
import com.pinterest.clusterservice.cm.DefaultClusterManager;
import com.pinterest.deployservice.db.*;
import com.pinterest.deployservice.events.DefaultEventSender;
import com.pinterest.teletraan.config.AWSFactory;
import com.pinterest.teletraan.config.AutoScalingFactory;
import com.pinterest.teletraan.config.EventSenderFactory;
import com.pinterest.teletraan.config.WorkerConfig;
import com.pinterest.teletraan.worker.*;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class ConfigHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigHelper.class);
    private static final int DEFAULT_PERIOD = 30;
    private static final int DEFAULT_MAX_STALE_HOST_THRESHOLD = 600;
    private static final int DEFAULT_MIN_STALE_HOST_THRESHOLD = 150;
    private static final int DEFAULT_LAUNCH_LATENCY_THRESHOLD = 600;
    private static final String DEFAULT_DEPLOY_JANITOR_SCHEDULE = "0 30 3 * * ?";
    private static final String DEFAULT_BUILD_JANITOR_SCHEDULE = "0 40 3 * * ?";
    private static final int DEFAULT_MAX_DAYS_TO_KEEP = 180;
    private static final int DEFAULT_MAX_BUILDS_TO_KEEP = 1000;

    public static TeletraanServiceContext setupContext(TeletraanServiceConfiguration configuration) throws Exception {
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
        context.setUtilDAO(new DBUtilDAOImpl(dataSource));

        context.setConfigHistoryDAO(new DBConfigHistoryDAOImpl(dataSource));
        context.setHostDAO(new DBHostDAOImpl(dataSource));
        context.setGroupDAO(new DBGroupDAOImpl(dataSource));
        context.setAgentDAO(new DBAgentDAOImpl(dataSource));
        context.setAgentErrorDAO(new DBAgentErrorDAOImpl(dataSource));

        context.setClusterDAO(new DBClusterDAOImpl(dataSource));
        context.setBaseImageDAO(new DBBaseImageDAOImpl(dataSource));
        context.setHostTypeDAO(new DBHostTypeDAOImpl(dataSource));
        context.setSecurityZoneDAO(new DBSecurityZoneDAOImpl(dataSource));
        context.setPlacementDAO(new DBPlacementDAOImpl(dataSource));
        context.setClusterUpgradeEventDAO(new DBClusterUpgradeEventDAOImpl(dataSource));
        context.setSpotAutoScalingDAO(new DBSpotAutoScalingDAOImpl(dataSource));
        context.setTagDAO(new DBTagDAOImpl(dataSource));

        // TODO Arcee specific
        context.setAlarmDAO(new DBAlarmDAOImpl(dataSource));
        context.setImageDAO(new DBImageDAOImpl(dataSource));
        context.setGroupInfoDAO(new DBGroupInfoDAOImpl(dataSource));
        context.setHealthCheckDAO(new DBHealthCheckDAOImpl(dataSource));
        context.setHealthCheckErrorDAO(new DBHealthCheckErrorDAOImpl(dataSource));
        context.setnewInstanceReportDAO(new DBNewInstanceReportDAOImpl(dataSource));
        context.setAsgLifecycleEventDAO(new DBAsgLifecycleEventDAOImpl(dataSource));
        context.setManagingGroupDAO(new DBManaginGroupDAOImpl(dataSource));

        // Inject proper implemetation based on config
        context.setAuthorizer(configuration.getAuthorizationFactory().create(context));
        context.setSourceControlManager(configuration.getSourceControlFactory().create());
        context.setChatManager(configuration.getChatFactory().create());
        context.setMailManager(configuration.getEmailFactory().createMailManager());
        context.setHostGroupDAO(configuration.getHostGroupFactory().createHostGroupDAO());
        context.setMetricSource(configuration.getMetricSourceFactory().create());

        EventSenderFactory eventSenderFactory = configuration.getEventSenderFactory();
        if (eventSenderFactory != null) {
            context.setEventSender(eventSenderFactory.createEventSender());
        } else {
            context.setEventSender(new DefaultEventSender());
        }

        // AWS specific DAOs
        AWSFactory awsFactory = configuration.getAwsFactory();
        if (awsFactory != null) {
            context.setAutoScalingManager(awsFactory.buildAwsAutoScalingManager());
            context.setAlarmManager(awsFactory.buildAwsAlarmManager());
            AmazonEC2Client ec2Client = awsFactory.buildEC2Client();
            context.setEc2Client(ec2Client);
            // TODO we should just use AwsConfigManager and get rid of the above 3
            context.setAwsConfigManager(awsFactory.buildAwsConfigManager());
            // TODO rename to manager
            context.setHostInfoDAO(new EC2HostInfoDAOImpl(ec2Client, context.getAwsConfigManager()));
            context.setReservedInstanceInfoDAO(new ReservedInstanceFetcher(ec2Client));
            context.setClusterManager(new AwsVmManager(context));
            context.setAwsManager(new AwsManagerImpl(context.getAwsConfigManager()));
        } else {
            // TODO make sure if aws is null, all the workers related to aws still works
            context.setHostInfoDAO(new DefaultHostInfoDAOImpl());
            context.setClusterManager(new DefaultClusterManager());
        }

        /**
         Lastly, let us create the in-process background job executor, all transient, long
         running background jobs can be handled by this executor
         Currently we hard coded the parameters as:

         corePoolSize - the number of threads to keep in the pool, even if they are idle, unless allowCoreThreadTimeOut is set
         maximumPoolSize - the maximum number of threads to allow in the pool
         keepAliveTime - when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating.
         unit - the time unit for the keepAliveTime argument
         workQueue - the queue to use for holding tasks before they are executed. This queue will hold only the Runnable tasks submitted by the execute method.
         */
        // TODO make the thread configrable
        ExecutorService jobPool = new ThreadPoolExecutor(1, 10, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        context.setJobPool(jobPool);

        context.setDeployBoardUrlPrefix(configuration.getSystemFactory().getDashboardUrl());
        context.setChangeFeedUrl(configuration.getSystemFactory().getChangeFeedUrl());

        AutoScalingFactory autoScalingFactory = configuration.getAutoScalingFactory();
        if (autoScalingFactory != null) {
            context.setQuboleAuthentication(autoScalingFactory.getQuboleAuthentication());
            context.setSpotAutoScalingThreshold(autoScalingFactory.getSpotAutoScalingThreshold());
        }

        return context;
    }

    public static void scheduleWorkers(TeletraanServiceConfiguration configuration, TeletraanServiceContext serviceContext) throws Exception {
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
                Runnable worker = new AutoPromoter(serviceContext);
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
                int minStaleHostThreshold = MapUtils.getIntValue(properties, "minStaleHostThreshold", DEFAULT_MIN_STALE_HOST_THRESHOLD);
                int maxStaleHostThreshold = MapUtils.getIntValue(properties, "maxStaleHostThreshold", DEFAULT_MAX_STALE_HOST_THRESHOLD);
                Runnable worker = new SimpleAgentJanitor(serviceContext, minStaleHostThreshold, maxStaleHostThreshold);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled SimpleAgentJanitor.");
            }

            if (workerName.equalsIgnoreCase(AgentJanitor.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                int minStaleHostThreshold = MapUtils.getIntValue(properties, "minStaleHostThreshold", DEFAULT_MIN_STALE_HOST_THRESHOLD);
                int maxStaleHostThreshold = MapUtils.getIntValue(properties, "maxStaleHostThreshold", DEFAULT_MAX_STALE_HOST_THRESHOLD);
                int maxLaunchLatencyThreshold = MapUtils.getIntValue(properties, "maxLaunchLaencyThreshold", DEFAULT_LAUNCH_LATENCY_THRESHOLD);
                Runnable worker = new AgentJanitor(serviceContext, minStaleHostThreshold, maxStaleHostThreshold, maxLaunchLatencyThreshold);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled AgentJanitor.");
            }

            // Schedule cron like jobs
            JobDetail deployJanitorJob = null;
            CronTrigger deployJanitorTrigger = null;
            if (workerName.equalsIgnoreCase(DeployJanitor.class.getSimpleName())) {
                String schedule = MapUtils.getString(properties, "schedule", DEFAULT_DEPLOY_JANITOR_SCHEDULE);
                deployJanitorJob = JobBuilder.newJob(DeployJanitor.class)
                        .withIdentity("deployJanitorJob", "group1")
                        .build();
                deployJanitorTrigger = TriggerBuilder.newTrigger()
                        .forJob(deployJanitorJob)
                        .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
                        .build();
            }
            JobDetail buildJanitorJob = null;
            CronTrigger buildJanitorTrigger = null;
            if (workerName.equalsIgnoreCase(BuildJanitor.class.getSimpleName())) {
                String schedule = MapUtils.getString(properties, "schedule", DEFAULT_BUILD_JANITOR_SCHEDULE);
                int maxDaysToKeep = MapUtils.getIntValue(properties, "minStaleHostThreshold", DEFAULT_MAX_DAYS_TO_KEEP);
                int maxBuildsToKeep = MapUtils.getIntValue(properties, "maxStaleHostThreshold", DEFAULT_MAX_BUILDS_TO_KEEP);
                serviceContext.setMaxDaysToKeep(maxDaysToKeep);
                serviceContext.setMaxBuildsToKeep(maxBuildsToKeep);
                buildJanitorJob = JobBuilder.newJob(BuildJanitor.class)
                        .withIdentity("buildJanitorJob", "group1")
                        .build();
                buildJanitorTrigger = TriggerBuilder.newTrigger()
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

            // TODO Arcee specific workers
            if (workerName.equalsIgnoreCase(LaunchLatencyUpdater.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new LaunchLatencyUpdater(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled LaunchLatencyUpdater.");
            }

            if (workerName.equalsIgnoreCase(MetricsCollector.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new MetricsCollector(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.SECONDS);
                LOG.info("Scheduled MetricsCollector.");
            }

            if (workerName.equalsIgnoreCase(GroupInfoUpdater.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new GroupInfoUpdater(serviceContext);
                int randomInitTime = new Random().nextInt(initDelay);
                scheduler.scheduleAtFixedRate(worker, randomInitTime, period, TimeUnit.MINUTES);
                LOG.info("Scheduled GroupInfoUpdater.");
            }

            if (workerName.equalsIgnoreCase(LaunchEventCollector.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new LaunchEventCollector(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled LaunchEventCollector.");
            }

            if (workerName.equalsIgnoreCase(HostTerminator.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new HostTerminator(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled HostTerminator.");
            }

            if (workerName.equalsIgnoreCase(HealthChecker.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new HealthChecker(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled HealthChecker.");
            }

            if (workerName.equalsIgnoreCase(HealthCheckInserter.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new HealthCheckInserter(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled HealthCheckInserter.");
            }

            if (workerName.equalsIgnoreCase(HealthCheckHostTerminator.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new HealthCheckHostTerminator(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled HealthCheckHostTerminator.");
            }

            if (workerName.equalsIgnoreCase(NewInstanceChecker.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new NewInstanceChecker(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled NewInstanceChecker.");
            }

            if (workerName.equalsIgnoreCase(LifecycleUpdator.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new LifecycleUpdator(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled LifecycleUpdator.");
            }

            if (workerName.equalsIgnoreCase(ReservedInstanceScheduler.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new ReservedInstanceScheduler(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled ReservedInstanceScheduler.");
            }

            if (workerName.equalsIgnoreCase(SpotAutoScalingScheduler.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new SpotAutoScalingScheduler(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled SpotAutoScalingScheduler");
            }

            if (workerName.equalsIgnoreCase(PlacementCapacityUpdater.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new PlacementCapacityUpdater(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled PlacementCapacityUpdater");
            }

            if (workerName.equalsIgnoreCase(ClusterReplacer.class.getSimpleName())) {
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                Runnable worker = new ClusterReplacer(serviceContext);
                scheduler.scheduleAtFixedRate(worker, initDelay, period, TimeUnit.MINUTES);
                LOG.info("Scheduled ClusterReplacer");
            }
        }
    }

}
