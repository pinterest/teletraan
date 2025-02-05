/**
 * Copyright (c) 2017-2024 Pinterest, Inc.
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

public enum TagSyncState {
    /** The initial state, environment is ready for tag sync workers */
    INIT,
    /**
     * Host_tags table is not in-sync with host ec2 tags, and tag sync workers are currently working
     * on it
     */
    PROCESSING,
    /** Failed to sync host_tags */
    ERROR,
    /** Currently host_tags table is in-sync with host ec2 tags in this environment */
    FINISHED
}
