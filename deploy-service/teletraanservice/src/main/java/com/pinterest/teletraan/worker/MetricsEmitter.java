/**
 * Copyright (c) 2023-2024 Pinterest, Inc.
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
package com.pinterest.teletraan.worker;

import static com.pinterest.teletraan.universal.metrics.micrometer.PinStatsNamingConvention.CUSTOM_NAME_PREFIX;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.HostAgentDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.metrics.DefaultHostClassifier;
import com.pinterest.deployservice.metrics.HostClassifier;
import com.pinterest.teletraan.universal.metrics.ErrorBudgetCounterFactory;
import com.pinterest.teletraan.universal.metrics.micrometer.PinStatsLongTaskTimer;
import com.pinterest.teletraan.universal.metrics.micrometer.PinStatsMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsEmitter implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsEmitter.class);

    static final String HOSTS_TOTAL = "hosts.total";
    static final String DEPLOYS_TODAY_TOTAL = "deploys.today.total";
    static final String DEPLOYS_RUNNING_TOTAL = "deploys.running.total";
    static final String HOSTS_LAUNCHING = CUSTOM_NAME_PREFIX + "teletraan.%s.hosts_launching";
    static final String ERROR_BUDGET_METHOD_NAME = "host_launch";
    static final int LAUNCH_TIMEOUT_MINUTE = 20;
    static final int MAX_TRACK_DURATION_MINUTE =
            LAUNCH_TIMEOUT_MINUTE * 5; // keep tracking for 5x timeout
    private static final Timer WORKER_TIMER =
            WorkerTimerFactory.createWorkerTimer(MetricsEmitter.class);

    static long reportHostsCount(HostAgentDAO hostAgentDAO) {
        try {
            return hostAgentDAO.getDistinctHostsCount();
        } catch (SQLException e) {
            LOG.error("Failed to get host count", e);
        }
        return 0;
    }

    static long reportDailyDeployCount(DeployDAO deployDAO) {
        try {
            return deployDAO.getDailyDeployCount();
        } catch (SQLException e) {
            LOG.error("Failed to get daily deploy count", e);
        }
        return 0;
    }

    static long reportRunningDeployCount(DeployDAO deployDAO) {
        try {
            return deployDAO.getRunningDeployCount();
        } catch (SQLException e) {
            LOG.error("Failed to get running deploy count", e);
        }
        return 0;
    }

    private final Clock clock;
    private final HostClassifier hostClassifier;
    private final HostDAO hostDAO;
    private final Map<String, LongTaskTimer.Sample> hostTimers = new HashMap<>();
    private final Counter errorBudgetSuccess;
    private final Counter errorBudgetFailure;

    public MetricsEmitter(ServiceContext serviceContext) {
        this(serviceContext, Clock.SYSTEM);
    }

    public MetricsEmitter(ServiceContext serviceContext, Clock clock) {
        this.clock = clock;
        // HostAgentDAO is more efficient than HostDAO to get total hosts
        Gauge.builder(
                        HOSTS_TOTAL,
                        serviceContext.getHostAgentDAO(),
                        MetricsEmitter::reportHostsCount)
                .strongReference(true)
                .register(Metrics.globalRegistry);
        Gauge.builder(
                        DEPLOYS_TODAY_TOTAL,
                        serviceContext.getDeployDAO(),
                        MetricsEmitter::reportDailyDeployCount)
                .strongReference(true)
                .register(Metrics.globalRegistry);
        Gauge.builder(
                        DEPLOYS_RUNNING_TOTAL,
                        serviceContext.getDeployDAO(),
                        MetricsEmitter::reportRunningDeployCount)
                .strongReference(true)
                .register(Metrics.globalRegistry);

        hostDAO = serviceContext.getHostDAO();
        hostClassifier = new DefaultHostClassifier();
        errorBudgetSuccess =
                ErrorBudgetCounterFactory.createSuccessCounter(ERROR_BUDGET_METHOD_NAME);
        errorBudgetFailure =
                ErrorBudgetCounterFactory.createFailureCounter(ERROR_BUDGET_METHOD_NAME);
    }

    @Override
    public void run() {
        WORKER_TIMER.record(() -> runInternal());
    }

    private void runInternal() {
        try {
            emitLaunchingMetrics();
        } catch (Exception e) {
            LOG.error("Failed to emit launching metrics", e);
        }
    }

    void emitLaunchingMetrics() {
        Instant timeoutCutoff =
                Instant.ofEpochMilli(clock.wallTime())
                        .minus(Duration.ofMinutes(LAUNCH_TIMEOUT_MINUTE));
        try {
            updateHostClassification(timeoutCutoff);
        } catch (Exception e) {
            LOG.error("Failed to update host classification", e);
        }
        processRemovedHosts();
        processNewHosts();
        cleanUpTimers();
    }

    private void updateHostClassification(Instant timeoutCutoff) {
        try {
            List<HostBean> agentlessHosts =
                    hostDAO.getAgentlessHosts(
                            Instant.ofEpochMilli(clock.wallTime())
                                    .minus(Duration.ofMinutes(MAX_TRACK_DURATION_MINUTE))
                                    .toEpochMilli(),
                            10000);
            hostClassifier.updateClassification(agentlessHosts, timeoutCutoff);
        } catch (SQLException e) {
            LOG.error("Failed to get agentless hosts", e);
        }
    }

    private void processRemovedHosts() {
        Collection<HostBean> removedHosts = hostClassifier.getRemovedHosts();
        for (HostBean host : removedHosts) {
            String hostId = host.getHost_id();

            if (hostTimers.containsKey(hostId)) {
                LongTaskTimer.Sample sample = hostTimers.remove(hostId);
                double sampleDurationMs = sample.duration(TimeUnit.MILLISECONDS);
                if (sampleDurationMs > Duration.ofMinutes(LAUNCH_TIMEOUT_MINUTE).toMillis()) {
                    // Only consider hosts that have been launched after timeout cutoff
                    errorBudgetFailure.increment();
                    LOG.info(
                            "Host {} launch time ({}ms) exceeded the launch timeout threshold",
                            hostId,
                            sampleDurationMs);
                } else {
                    errorBudgetSuccess.increment();
                }
                sample.stop();
            } else {
                LOG.warn("Timer for removed host {} not found, skip", hostId);
            }
        }
    }

    private void processNewHosts() {
        // Only `PinStatsMeterRegistry` can create `PinStatsLongTaskTimer`
        PinStatsMeterRegistry registry =
                (PinStatsMeterRegistry)
                        Metrics.globalRegistry.getRegistries().stream()
                                .filter(r -> r instanceof PinStatsMeterRegistry)
                                .findFirst()
                                .get();
        if (registry != null) {
            Collection<HostBean> newHosts = hostClassifier.getNewHosts();
            for (HostBean host : newHosts) {
                String timerName = String.format(HOSTS_LAUNCHING, host.getGroup_name());
                PinStatsLongTaskTimer timer =
                        (PinStatsLongTaskTimer)
                                LongTaskTimer.builder(timerName)
                                        .serviceLevelObjectives(
                                                Duration.ofMinutes(LAUNCH_TIMEOUT_MINUTE))
                                        .register(registry);
                hostTimers.put(
                        host.getHost_id(),
                        timer.start(Instant.ofEpochMilli(host.getCreate_date())));
            }
        }
    }

    /*
     * Clean up timers for hosts that have been initializing for too long
     */
    private void cleanUpTimers() {
        Iterator<Map.Entry<String, LongTaskTimer.Sample>> iterator =
                hostTimers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, LongTaskTimer.Sample> entry = iterator.next();
            String hostId = entry.getKey();
            LongTaskTimer.Sample sample = entry.getValue();

            if (sample.duration(TimeUnit.MINUTES) > (double) MAX_TRACK_DURATION_MINUTE) {
                sample.stop();
                iterator.remove();
                errorBudgetFailure.increment();
                LOG.info("Removed timer for host {} after max tracking duration", hostId);
            }
        }
    }
}
