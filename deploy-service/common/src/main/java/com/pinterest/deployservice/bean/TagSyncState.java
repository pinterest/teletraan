package com.pinterest.deployservice.bean;

/**
 * INIT:
 *      the initial state, environment is ready for tag sync workers
 * PROCESSING:
 *      host_tags table is not in-sync with host ec2 tags, and tag sync workers are currently working on it
 * ERROR:
 *      failed to sync host_tags
 * FINISHED:
 *      currently host_tags table is in-sync with host ec2 tags in this environment.
 */
public enum TagSyncState {
    INIT,
    PROCESSING,
    ERROR,
    FINISHED
}
