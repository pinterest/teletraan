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

import com.pinterest.clusterservice.bean.SecurityZoneBean;

import java.util.Collection;

public interface SecurityZoneDAO {
    void insert(SecurityZoneBean bean) throws Exception;

    SecurityZoneBean getById(String id) throws Exception;

    SecurityZoneBean getByProviderAndAbstractName(String provider, String abstractName) throws Exception;

    Collection<SecurityZoneBean> getAll(int pageIndex, int pageSize) throws Exception;

    Collection<SecurityZoneBean> getByProvider(String provider) throws Exception;
}
