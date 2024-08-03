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

import com.pinterest.deployservice.bean.HostBean;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface HostClassifier {

    /**
     * Retrieves hosts that are newly added.
     *
     * <p>Note this is a subset of hosts that are initializing.
     *
     * @return a list of newly added hosts
     */
    List<HostBean> getNewHosts();

    /**
     * Retrieves hosts that are carried over from last update.
     *
     * <p>Note this is a subset of hosts that are initializing.
     *
     * @return a list of carried over hosts
     */
    List<HostBean> getCarryoverHosts();

    /**
     * Retrieves hosts that have timed out.
     *
     * <p>Note this is a subset of hosts that are initializing.
     *
     * @return a list of hosts that have timed out
     */
    List<HostBean> getTimeoutHosts();

    /**
     * Retrieves hosts that have been removed.
     *
     * <p>Specifically, a previously initializing host that is no longer in the provided host list.
     * A host can be absent from the provided host list for 2 reasons: 1. It has been initialized 2.
     * It has been taking too long to initialize
     *
     * @return a list of hosts that have been removed
     */
    List<HostBean> getRemovedHosts();

    /**
     * Retrieves hosts that are currently initializing.
     *
     * <p>Note this is the union of newly added and carryover hosts.
     *
     * @return a list of hosts that are currently initializing
     */
    List<HostBean> getInitializingHosts();

    /**
     * Updates the classification of hosts.
     *
     * @param hosts the collection of hosts to update the classification with
     * @param timeoutInstant the instant used to determine the timeout hosts
     */
    void updateClassification(Collection<HostBean> hosts, Instant timeoutInstant);
}
