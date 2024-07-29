/**
 * Copyright 2016 Pinterest, Inc.
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
package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.DataBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.common.PersistableJSONFactory;
import com.pinterest.deployservice.dao.DataDAO;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;

public class DataHandler {
    /**
     * TODO we should save everything in json format, and get rid of STRING AND MAP
     * But the migration would be hard though
     */
    public enum DataType {
        MAP, // TODO do not use this, deprecated
        JSON
    }

    private DataDAO dataDAO;

    public DataHandler(ServiceContext serviceContext) {
        dataDAO = serviceContext.getDataDAO();
    }

    public Map<String, String> getMapById(String id) throws Exception {
        if (StringUtils.isEmpty(id)) {
            return Collections.emptyMap();
        }
        DataBean bean = dataDAO.getById(id);
        if (bean != null) {
            return CommonUtils.decodeData(bean.getData());
        }
        return Collections.EMPTY_MAP;
    }

    public String insertMap(Map<String, String> data, String operator) throws Exception {
        return insertData(CommonUtils.encodeData(data), DataType.MAP, operator);
    }

    public void updateMap(String id, Map<String, String> data, String operator) throws Exception {
        updateData(id, CommonUtils.encodeData(data), operator);
    }

    public <T> T getDataById(String id, Class<? extends PersistableJSONFactory<T>> clazz) throws Exception {
        PersistableJSONFactory<T> factory = clazz.newInstance();
        if (StringUtils.isEmpty(id)) {
            return factory.fromJson(null);
        }
        DataBean dataBean = dataDAO.getById(id);
        if (dataBean == null) {
            return factory.fromJson(null);
        }
        return factory.fromJson(dataBean.getData());
    }

    public <T> String insertData(T data, Class<? extends PersistableJSONFactory<T>> clazz, String operator) throws Exception {
        PersistableJSONFactory<T> factory = clazz.newInstance();
        return insertData(factory.toJson(data), DataType.JSON, operator);
    }

    public <T> void insertOrUpdateData(String id, T data, Class<? extends PersistableJSONFactory<T>> clazz, String operator) throws Exception {
        PersistableJSONFactory<T> factory = clazz.newInstance();
        DataBean dataBean = new DataBean();
        dataBean.setData(factory.toJson(data));
        dataBean.setData_id(id);
        dataBean.setData_kind(DataType.JSON.toString());
        dataBean.setTimestamp(System.currentTimeMillis());
        dataBean.setOperator(operator);
        dataDAO.insertOrUpdate(id, dataBean);
    }

    public <T> void updateData(String id, T data, Class<? extends PersistableJSONFactory<T>> clazz, String operator) throws Exception {
        PersistableJSONFactory<T> factory = clazz.newInstance();
        updateData(id, factory.toJson(data), operator);
    }

    public void deleteData(String id) throws Exception {
        dataDAO.delete(id);
    }

    private String insertData(String data, DataType kind, String operator) throws Exception {
        DataBean dataBean = new DataBean();
        String id = CommonUtils.getBase64UUID();
        dataBean.setData_id(id);
        dataBean.setData_kind(kind.toString());
        dataBean.setOperator(operator);
        dataBean.setTimestamp(System.currentTimeMillis());
        dataBean.setData(data);
        dataDAO.insert(dataBean);
        return id;
    }

    private void updateData(String id, String data, String operator) throws Exception {
        DataBean dataBean = new DataBean();
        dataBean.setOperator(operator);
        dataBean.setTimestamp(System.currentTimeMillis());
        dataBean.setData(data);
        dataDAO.update(id, dataBean);
    }
}
