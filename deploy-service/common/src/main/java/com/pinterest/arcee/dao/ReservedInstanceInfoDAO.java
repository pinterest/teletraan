package com.pinterest.arcee.dao;


import com.pinterest.arcee.bean.ReservedInstanceBean;
import java.util.Collection;

public interface ReservedInstanceInfoDAO {
    Collection<ReservedInstanceBean> getAllReservedInstanceInfo() throws Exception;

    int getReservedInstanceCount(String instanceType) throws Exception;

    int getRunningReservedInstanceCount(String instanceType) throws Exception;
}
