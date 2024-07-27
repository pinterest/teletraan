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

import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import java.util.List;

public interface UserRolesDAO {
    void insert(UserRolesBean bean) throws Exception;

    void delete(String userName, String resourceId, AuthZResource.Type resourceType) throws Exception;

    void update(UserRolesBean bean, String userName, String resourceId, AuthZResource.Type resourceType)
            throws Exception;

    UserRolesBean getByNameAndResource(String userName, String resourceId, AuthZResource.Type resourceType)
            throws Exception;

    List<UserRolesBean> getByResource(String resourceId, AuthZResource.Type resourceType) throws Exception;
}
