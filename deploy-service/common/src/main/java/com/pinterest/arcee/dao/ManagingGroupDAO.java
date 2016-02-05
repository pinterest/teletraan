package com.pinterest.arcee.dao;


import com.pinterest.arcee.bean.ManagingGroupsBean;

public interface ManagingGroupDAO {
    void insertManagingGroup(ManagingGroupsBean managingGroupsBean) throws Exception;

    ManagingGroupsBean getManagingGroupByGroupName(String groupName) throws Exception;

    void updateManagingGroup(String groupName, ManagingGroupsBean managingGroupsBean) throws Exception;

    void deleteManagingGroup(String groupName) throws Exception;
}
