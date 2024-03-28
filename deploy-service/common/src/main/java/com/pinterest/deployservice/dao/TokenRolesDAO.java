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

import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import java.util.List;

public interface TokenRolesDAO {
    void insert(TokenRolesBean bean) throws Exception;

    void delete(String scriptName, String resourceId, AuthZResource.Type resourceType) throws Exception;

    void update(TokenRolesBean bean, String scriptName, String resourceId, AuthZResource.Type resourceType)
            throws Exception;

    TokenRolesBean getByToken(String token) throws Exception;

    TokenRolesBean getByNameAndResource(String scriptName, String resourceId, AuthZResource.Type resourceType)
            throws Exception;

    List<TokenRolesBean> getByResource(String resourceId, AuthZResource.Type resourceType) throws Exception;
}
