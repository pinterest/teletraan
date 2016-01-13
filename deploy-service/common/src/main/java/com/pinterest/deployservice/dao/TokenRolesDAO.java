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
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.TokenRolesBean;

import java.util.List;

public interface TokenRolesDAO {
    public void insert(TokenRolesBean bean) throws Exception;

    public void delete(String scriptName, String resourceId,
        Resource.Type resourceType) throws Exception;

    public void update(TokenRolesBean bean, String scriptName, String resourceId,
        Resource.Type resourceType) throws Exception;

    public TokenRolesBean getByToken(String token) throws Exception;

    public TokenRolesBean getByNameAndResource(String scriptName, String resourceId,
        Resource.Type resourceType) throws Exception;

    public List<TokenRolesBean> getByResource(String resourceId,
        Resource.Type resourceType) throws Exception;
}
