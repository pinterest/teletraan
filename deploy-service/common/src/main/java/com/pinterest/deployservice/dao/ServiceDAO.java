/*
 * Copyright 2021 Pinterest, Inc.
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
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.ServiceBean;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ServiceDAO {
    void insert(ServiceBean serviceBean) throws Exception;

    void update(String name, ServiceBean serviceBean) throws Exception;

    void delete(String name) throws Exception;

    ServiceBean get(String name) throws Exception;

}
