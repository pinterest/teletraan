package com.pinterest.arcee.dao;

import com.pinterest.arcee.bean.PasConfigBean;

public interface PasConfigDAO {

    PasConfigBean getPasConfig(String groupName) throws Exception;

    void updatePasConfig(PasConfigBean pasConfigBean) throws Exception;

    void insertPasConfig(PasConfigBean pasConfigBean) throws Exception;
}
