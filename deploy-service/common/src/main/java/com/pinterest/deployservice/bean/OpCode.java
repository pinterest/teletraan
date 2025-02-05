/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

public enum OpCode {
    /** No action needed */
    NOOP,
    /** Agent needs to deploy service */
    DEPLOY,
    /** Agent needs to restart service */
    RESTART,
    /** Agent needs to delete its own status file for this env */
    DELETE,
    /** Agent needs to sleep for certain time */
    WAIT,
    /** Agent needs to rollback */
    ROLLBACK,
    /** Agent needs to shutdown service */
    STOP
}
