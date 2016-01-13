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
package com.pinterest.deployservice.bean;

/**
 * READER:
 *      Default role, everyone who is able to use Teletraan has READER access.
 * PINGER:
 *      Role required to ping server.
 * PUBLISHER:
 *      Role required to publish artifacts.
 * OPERATOR:
 *      Role where user can modify a specific environment's config and
 *      perform deploy related actions.
 * ADMIN:
 *      Role that has the same environment specific privileges as OPERATOR
 *      plus the ability specify new OPERATORS and ADMINs for said environment.
 *      When a new environment is created the creating user is the designated the
 *      first ADMIN.
 */
public enum Role {
    READER(0),
    PINGER(1),
    PUBLISHER(1),
    OPERATOR(10),
    ADMIN(20);

    private int value;

    private Role(int value) {
        this.value = value;
    }

    public boolean isAuthorized(Role requiredRole) {
        return this == requiredRole || this.value > requiredRole.value;
    }
}
