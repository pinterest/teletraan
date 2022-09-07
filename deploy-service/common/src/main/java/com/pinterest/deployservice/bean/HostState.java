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
 * PROVISIONED:
 *      Host is being provisioned
 * ACTIVE:
 *      Host is ready to deploy
 * PENDING_TERMINATE:
 *      Host is pending terminate
 * TERMINATING:
 *      Host if being terminated
 * TERMINATED:
 *      Host is terminated
 * PENDING_REPLACEABLE_TERMINATE:
 *      Host is pending terminate with replacement request
 */
public enum HostState {
    PROVISIONED,
    ACTIVE,
    PENDING_TERMINATE,
    TERMINATING,
    TERMINATED,
    PENDING_REPLACEABLE_TERMINATE
}
