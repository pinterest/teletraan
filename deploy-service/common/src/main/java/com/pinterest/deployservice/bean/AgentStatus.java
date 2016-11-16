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

public enum AgentStatus {
    /*
     * Deploy succeeded
     */
    SUCCEEDED,
    /*
     * Deploy status is unknown
     */
    UNKNOWN,
    /*
     * Deploy agent failure, not retryable
     */
    AGENT_FAILED,
    /*
     * Deploy agent failed, but could retry
     */
    RETRYABLE_AGENT_FAILED,
    /*
     * Deploy script failed This is not fatal
     */
    SCRIPT_FAILED,
    /*
     * Agent execution is aborted manually
     */
    ABORTED_BY_SERVICE,
    /*
     * Deploy script execution timed out
     */
    SCRIPT_TIMEOUT,
    /*
     * Deploy agent failed with too many retries
     */
    TOO_MANY_RETRY,
    /*
     * Runtime release version mismatch
     */
    RUNTIME_MISMATCH
}
