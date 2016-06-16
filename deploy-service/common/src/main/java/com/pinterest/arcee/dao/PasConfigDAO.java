package com.pinterest.arcee.dao;

import com.pinterest.arcee.bean.PasConfigBean;

import java.util.List;

public interface PasConfigDAO {

    PasConfigBean getPasConfig(String groupName) throws Exception;

    void updatePasConfig(PasConfigBean pasConfigBean) throws Exception;

    void insertPasConfig(PasConfigBean pasConfigBean) throws Exception;

    List<String> getAllPasGroups() throws Exception;

    List<PasConfigBean> getEnabledPasConfigs() throws Exception;
}
