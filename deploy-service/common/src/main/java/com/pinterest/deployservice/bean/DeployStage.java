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
 * UNKNOWN:
 *      Reserved by system when deploy stage is unknown
 * PRE_DOWNLOAD:
 *      Before deploy agent download the build
 * DOWNLOADING:
 *      Deploy agent is downloading the build
 * POST_DOWNLOAD:
 *      After deploy agent download the build
 * STAGING:
 *      Deploy agent is working on prepare the build for service restart
 * PRE_RESTART:
 *      Before deploy agent restart the service
 * RESTARTING:
 *      Deploy agent is restarting the service
 * POST_RESTART:
 *      After deploy agent restart the service
 * SERVING_BUILD:
 *      Service is serving traffic
 * STOPPING:
 *      Deploy Agent is shutting down the service
 * STOPPED:
 *      Complete shutting down the service
 */
public enum DeployStage {
    UNKNOWN,
    PRE_DOWNLOAD,
    DOWNLOADING,
    POST_DOWNLOAD,
    STAGING,
    PRE_RESTART,
    RESTARTING,
    POST_RESTART,
    SERVING_BUILD,
    STOPPING,
    STOPPED
}
