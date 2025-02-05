/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

public enum AgentState {
    /** Normal state */
    NORMAL,
    /** Agent being paused by system automatically */
    PAUSED_BY_SYSTEM,
    /** Agent being paused by user manually */
    PAUSED_BY_USER,
    /** Agent should retry last failure */
    RESET,
    /** Agent triggers redeploy */
    RESET_BY_SYSTEM,
    /** Agent should delete its status file */
    DELETE,
    /** Agent has not reported for certain time */
    UNREACHABLE,
    /** Agent is shutting down the service */
    STOP
}
