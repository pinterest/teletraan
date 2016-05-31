/*
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
package com.pinterest.clusterservice.dao;

import com.pinterest.clusterservice.bean.PlacementBean;

import java.util.Collection;

public interface PlacementDAO {
    void insert(PlacementBean bean) throws Exception;

    void updateById(String id, PlacementBean bean) throws Exception;

    PlacementBean getById(String id) throws Exception;

    PlacementBean getByProviderAndAbstractName(String provider, String abstractName) throws Exception;

    Collection<PlacementBean> getAll(int pageIndex, int pageSize) throws Exception;

    Collection<PlacementBean> getByProvider(String provider) throws Exception;
}
