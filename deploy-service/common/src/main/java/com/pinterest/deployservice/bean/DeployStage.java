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

public enum DeployStage {
    /** Reserved by system when deploy stage is unknown */
    UNKNOWN,
    /** Before deploy agent download the build */
    PRE_DOWNLOAD,
    /** Deploy agent is downloading the build */
    DOWNLOADING,
    /** After deploy agent download the build */
    POST_DOWNLOAD,
    /** Deploy agent is working on prepare the build for service restart */
    STAGING,
    /** Before deploy agent restart the service */
    PRE_RESTART,
    /** Deploy agent is restarting the service */
    RESTARTING,
    /** After deploy agent restart the service */
    POST_RESTART,
    /** Service is serving traffic */
    SERVING_BUILD,
    /** Deploy Agent is shutting down the service */
    STOPPING,
    /** Complete shutting down the service */
    STOPPED
}
