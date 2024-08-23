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

import static com.pinterest.deployservice.bean.BeanUtils.createHostBean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.deployservice.bean.HostBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultHostClassifierTest {

    private DefaultHostClassifier sut;
    private final Instant timeoutInstant = Instant.now().minus(Duration.ofMinutes(1l));
    private List<HostBean> regularHost;
    private List<HostBean> timeoutHost;

    @BeforeEach
    public void setUp() {
        sut = new DefaultHostClassifier();
        regularHost = Collections.singletonList(createHostBean(Instant.now()));
        timeoutHost =
                Collections.singletonList(
                        createHostBean(Instant.now().minus(Duration.ofMinutes(2l))));
    }

    @Test
    public void testGetCarryoverHosts() {
        assertEquals(0, sut.getCarryoverHosts().size());
    }

    @Test
    public void testGetRemovedHosts() {
        assertEquals(0, sut.getRemovedHosts().size());
    }

    @Test
    public void testGetInitializingHosts() {
        assertEquals(0, sut.getInitializingHosts().size());
    }

    @Test
    public void testGetNewHosts() {
        assertEquals(0, sut.getNewHosts().size());
    }

    @Test
    public void testGetTimeoutHosts() {
        assertEquals(0, sut.getTimeoutHosts().size());
    }

    @Test
    public void oneInvocation_regularHost() {
        sut.updateClassification(regularHost, timeoutInstant);

        assertTrue(CollectionUtils.isEqualCollection(regularHost, sut.getNewHosts()));
        assertTrue(CollectionUtils.isEqualCollection(regularHost, sut.getInitializingHosts()));
        assertEquals(0, sut.getTimeoutHosts().size());
        assertEquals(0, sut.getCarryoverHosts().size());
        assertEquals(0, sut.getRemovedHosts().size());
    }

    @Test
    public void oneInvocation_timeOutHost() {
        sut.updateClassification(timeoutHost, timeoutInstant);

        assertTrue(CollectionUtils.isEqualCollection(timeoutHost, sut.getNewHosts()));
        assertTrue(CollectionUtils.isEqualCollection(timeoutHost, sut.getInitializingHosts()));
        assertTrue(CollectionUtils.isEqualCollection(timeoutHost, sut.getTimeoutHosts()));
        assertEquals(0, sut.getCarryoverHosts().size());
        assertEquals(0, sut.getRemovedHosts().size());
    }

    @Test
    public void twoInvocations_sameHost() {
        sut.updateClassification(regularHost, timeoutInstant);
        sut.updateClassification(regularHost, timeoutInstant);

        assertEquals(0, sut.getNewHosts().size());
        assertTrue(CollectionUtils.isEqualCollection(regularHost, sut.getInitializingHosts()));
        assertEquals(0, sut.getTimeoutHosts().size());
        assertTrue(CollectionUtils.isEqualCollection(regularHost, sut.getCarryoverHosts()));
        assertEquals(0, sut.getRemovedHosts().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void twoInvocations_newRegularHost() {
        List<HostBean> secondHost = Collections.singletonList(createHostBean(Instant.now()));
        List<HostBean> allHosts = (List<HostBean>) CollectionUtils.union(regularHost, secondHost);

        sut.updateClassification(regularHost, timeoutInstant);
        sut.updateClassification(allHosts, timeoutInstant);

        assertTrue(CollectionUtils.isEqualCollection(secondHost, sut.getNewHosts()));
        assertTrue(CollectionUtils.isEqualCollection(allHosts, sut.getInitializingHosts()));
        assertEquals(0, sut.getTimeoutHosts().size());
        assertTrue(CollectionUtils.isEqualCollection(regularHost, sut.getCarryoverHosts()));
        assertEquals(0, sut.getRemovedHosts().size());
    }

    @Test
    public void twoInvocations_newRegularHostOnly() {
        List<HostBean> secondHost = Collections.singletonList(createHostBean(Instant.now()));

        sut.updateClassification(regularHost, timeoutInstant);
        sut.updateClassification(secondHost, timeoutInstant);

        assertTrue(CollectionUtils.isEqualCollection(secondHost, sut.getNewHosts()));
        assertTrue(CollectionUtils.isEqualCollection(secondHost, sut.getInitializingHosts()));
        assertEquals(0, sut.getTimeoutHosts().size());
        assertEquals(0, sut.getCarryoverHosts().size());
        assertTrue(CollectionUtils.isEqualCollection(regularHost, sut.getRemovedHosts()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void twoInvocations_timeoutHost() {
        List<HostBean> allHosts = (List<HostBean>) CollectionUtils.union(regularHost, timeoutHost);
        sut.updateClassification(regularHost, timeoutInstant);
        sut.updateClassification(allHosts, timeoutInstant);

        assertTrue(CollectionUtils.isEqualCollection(timeoutHost, sut.getNewHosts()));
        assertTrue(CollectionUtils.isEqualCollection(allHosts, sut.getInitializingHosts()));
        assertTrue(CollectionUtils.isEqualCollection(timeoutHost, sut.getTimeoutHosts()));
        assertTrue(CollectionUtils.isEqualCollection(regularHost, sut.getCarryoverHosts()));
        assertEquals(0, sut.getRemovedHosts().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void twoInvocations_timeoutHostCompleted() {
        sut.updateClassification(timeoutHost, timeoutInstant);
        sut.updateClassification(CollectionUtils.EMPTY_COLLECTION, timeoutInstant);

        assertEquals(0, sut.getNewHosts().size());
        assertEquals(0, sut.getInitializingHosts().size());
        assertEquals(0, sut.getTimeoutHosts().size());
        assertEquals(0, sut.getCarryoverHosts().size());
        assertTrue(CollectionUtils.isEqualCollection(timeoutHost, sut.getRemovedHosts()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void twoInvocations_complex() {
        List<HostBean> carryOverHost = Collections.singletonList(createHostBean(Instant.now()));
        List<HostBean> newHost = Collections.singletonList(createHostBean(Instant.now()));
        List<HostBean> agentLessHosts = new ArrayList<>(carryOverHost);
        agentLessHosts.addAll(newHost);
        agentLessHosts.addAll(timeoutHost);

        sut.updateClassification(CollectionUtils.union(regularHost, carryOverHost), timeoutInstant);
        sut.updateClassification(agentLessHosts, timeoutInstant);

        assertTrue(
                CollectionUtils.isEqualCollection(
                        CollectionUtils.union(newHost, timeoutHost), sut.getNewHosts()));
        assertTrue(CollectionUtils.isEqualCollection(agentLessHosts, sut.getInitializingHosts()));
        assertTrue(CollectionUtils.isEqualCollection(timeoutHost, sut.getTimeoutHosts()));
        assertTrue(CollectionUtils.isEqualCollection(carryOverHost, sut.getCarryoverHosts()));
        assertTrue(CollectionUtils.isEqualCollection(regularHost, sut.getRemovedHosts()));
    }

    @Test
    public void duplicateHostId_earliestHostOnly() {
        List<HostBean> hosts = new ArrayList<>(regularHost);
        HostBean laterHost = createHostBean(Instant.now());
        HostBean earlierHost =
                createHostBean(Instant.ofEpochMilli(regularHost.get(0).getCreate_date() - 1000));

        laterHost.setHost_id(regularHost.get(0).getHost_id());
        earlierHost.setHost_id(regularHost.get(0).getHost_id());

        hosts.add(laterHost);
        hosts.add(earlierHost);
        List<HostBean> expectedHosts = Collections.singletonList(earlierHost);

        sut.updateClassification(hosts, timeoutInstant);

        assertTrue(CollectionUtils.isEqualCollection(expectedHosts, sut.getNewHosts()));
        assertTrue(CollectionUtils.isEqualCollection(expectedHosts, sut.getInitializingHosts()));
        assertEquals(0, sut.getTimeoutHosts().size());
        assertEquals(0, sut.getCarryoverHosts().size());
        assertEquals(0, sut.getRemovedHosts().size());
    }
}
