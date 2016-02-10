package com.pinterest.arcee.dao;


public interface LeaseDAO {
    void lendInstances(String cluster, int count) throws Exception;

    void returnInstances(String cluster, int count) throws Exception;
}
