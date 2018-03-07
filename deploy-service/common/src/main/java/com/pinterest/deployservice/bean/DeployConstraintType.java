package com.pinterest.deployservice.bean;

/**
 * GROUP_BY_GROUP:
 *      randomly choose X num of hosts from one group, and ONLY deploy this group,
 *      when all the hosts in this group finish, proceed to the next group
 * ALL_GROUPS_IN_PARALLEL:
 *      randomly choose X num of hosts from EACH group, and deploy at the same time
 */
public enum  DeployConstraintType {
    GROUP_BY_GROUP,
    ALL_GROUPS_IN_PARALLEL
}
