/**
 * Copyright (c) 2024 Pinterest, Inc.
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
package com.pinterest.deployservice.metrics;

import com.google.common.collect.Maps;
import com.pinterest.deployservice.bean.HostBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHostClassifier implements HostClassifier {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultHostClassifier.class);

    private List<HostBean> carryoverHosts = new ArrayList<>();
    private List<HostBean> newHosts = new ArrayList<>();
    private List<HostBean> removedHosts = new ArrayList<>();
    private List<HostBean> timeoutHosts = new ArrayList<>();
    private @Nonnull List<HostBean> initializingHosts = new ArrayList<>();

    @Override
    public List<HostBean> getTimeoutHosts() {
        return timeoutHosts;
    }

    @Override
    public List<HostBean> getRemovedHosts() {
        return removedHosts;
    }

    @Override
    public List<HostBean> getNewHosts() {
        return newHosts;
    }

    @Override
    public List<HostBean> getCarryoverHosts() {
        return carryoverHosts;
    }

    @Override
    public List<HostBean> getInitializingHosts() {
        return initializingHosts;
    }

    private Map<String, HostBean> getInitializingHostMap() {
        return Maps.uniqueIndex(initializingHosts, HostBean::getHost_id);
    }

    @Override
    public void updateClassification(Collection<HostBean> agentlessHosts, Instant timeoutInstant) {
        Map<String, HostBean> uniqueAgentlessHostMap = deduplicateHosts(agentlessHosts);
        Map<String, HostBean> previousInitializingHosts = getInitializingHostMap();
        Map<String, HostBean> removedHostMap = new HashMap<>(previousInitializingHosts);

        List<HostBean> newTimeoutHosts = new ArrayList<>();
        List<HostBean> newlyLaunchedHosts = new ArrayList<>();
        List<HostBean> newCarryoverHosts = new ArrayList<>();

        initializingHosts = new ArrayList<>(uniqueAgentlessHostMap.values());
        for (HostBean host : initializingHosts) {
            removedHostMap.remove(host.getHost_id());
            Instant hostCreationInstant = Instant.ofEpochMilli(host.getCreate_date());
            if (hostCreationInstant.isBefore(timeoutInstant)) {
                newTimeoutHosts.add(host);
            }
            if (previousInitializingHosts.containsKey(host.getHost_id())) {
                newCarryoverHosts.add(host);
            } else {
                newlyLaunchedHosts.add(host);
            }
        }

        removedHosts = new ArrayList<>(removedHostMap.values());
        carryoverHosts = newCarryoverHosts;
        timeoutHosts = newTimeoutHosts;
        newHosts = newlyLaunchedHosts;

        LOG.info(
                "Host classification of {} agentless hosts based on {} previous initializing hosts: {} new, {} carryover, {} removed, {} timeout, {} initializing",
                agentlessHosts.size(),
                previousInitializingHosts.size(),
                newHosts.size(),
                carryoverHosts.size(),
                removedHosts.size(),
                timeoutHosts.size(),
                initializingHosts.size());
    }

    private Map<String, HostBean> deduplicateHosts(Collection<HostBean> agentlessHosts) {
        Map<String, HostBean> uniqueHosts = new HashMap<>();
        for (HostBean host : agentlessHosts) {
            if (!uniqueHosts.containsKey(host.getHost_id())
                    || host.getCreate_date()
                            < uniqueHosts.get(host.getHost_id()).getCreate_date()) {
                uniqueHosts.put(host.getHost_id(), host);
            }
        }
        return uniqueHosts;
    }
}
