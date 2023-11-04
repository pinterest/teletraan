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
 * NORMAL:
 *      Normal state
 * PAUSED_BY_SYSTEM:
 *      Agent being paused by system automatically
 * PAUSED_BY_USER:
 *      Agent being paused by user manually
 * RESET:
 *      Agent should retry last failure
 * RESET_BY_SYSTEM:
 *      Agent triggers redeploy 
 * DELETE:
 *      Agent should delete its status file
 * UNREACHABLE:
 *      Agent has not reported for certain time
 * STOP:
 *      Agent is shutting down the service
 *
 */
public enum AgentState {
    NORMAL,
    PAUSED_BY_SYSTEM,
    PAUSED_BY_USER,
    RESET,
    RESET_BY_SYSTEM,
    DELETE,
    UNREACHABLE,
    STOP
}
