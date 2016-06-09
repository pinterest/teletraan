package com.pinterest.arcee.dao;

import com.pinterest.arcee.bean.GroupMappingBean;

import java.util.Collection;

public interface GroupMappingDAO {
    void insertGroupMapping(String asgGroupName, GroupMappingBean groupMappingsBean) throws Exception;

    void updateGroupMapping(String asgGroupName, GroupMappingBean groupMappingsBean) throws Exception;

    void deleteGroupMapping(String asgGroupName) throws Exception;

    GroupMappingBean getGroupMappingByAsgGroupName(String asgGroupName) throws Exception;

    Collection<GroupMappingBean> getGroupMappingsByCluster(String clusterName) throws Exception;
}
