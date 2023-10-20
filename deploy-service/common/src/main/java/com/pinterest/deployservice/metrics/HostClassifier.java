package com.pinterest.deployservice.metrics;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.pinterest.deployservice.bean.HostBean;

public interface HostClassifier {

    /**
     * Retrieves hosts that are newly added.
     *
     * Note this is a subset of hosts that are initializing.
     *
     * @return a list of newly added hosts
     */
    List<HostBean> getNewHosts();

    /**
     * Retrieves hosts that are carried over from last update.
     *
     * Note this is a subset of hosts that are initializing.
     *
     * @return a list of carried over hosts
     */
    List<HostBean> getCarryoverHosts();

    /**
     * Retrieves hosts that have timed out.
     *
     * Note this is a subset of hosts that are initializing.
     *
     * @return a list of hosts that have timed out
     */
    List<HostBean> getTimeoutHosts();

    /**
     * Retrieves hosts that have been removed.
     *
     * Specifically, a previously initializing host that is no longer in the
     * provided host list.
     * A host can be absent from the provided host list for 2 reasons:
     * 1. It has been initialized
     * 2. It has been taking too long to initialize
     *
     * @return a list of hosts that have been removed
     */
    List<HostBean> getRemovedHosts();

    /**
     * Retrieves hosts that are currently initializing.
     *
     * Note this is the union of newly added and carryover hosts.
     *
     * @return a list of hosts that are currently initializing
     */
    List<HostBean> getInitializingHosts();

    /**
     * Updates the classification of hosts.
     *
     * @param hosts          the collection of hosts to update the classification
     *                       with
     * @param timeoutInstant the instant used to determine the timeout hosts
     */
    void updateClassification(Collection<HostBean> hosts, Instant timeoutInstant);
}