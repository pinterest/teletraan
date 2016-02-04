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
package com.pinterest.clusterservice.dao;

import com.pinterest.clusterservice.bean.PlacementBean;

public interface PlacementDAO {
    public void insert(PlacementBean bean) throws Exception;

    public PlacementBean getById(String id) throws Exception;

    public PlacementBean getByProvider(String provider) throws Exception;
}
