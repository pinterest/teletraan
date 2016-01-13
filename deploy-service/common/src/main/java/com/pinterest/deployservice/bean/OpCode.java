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
 * NOOP:
 *      No action needed
 * DEPLOY:
 *      Agent needs to restart service
 * RESTART:
 *      Agent needs to restart service
 * DELETE:
 *      Agent needs to delete its own status file for this env
 * WAIT:
 *      Agent needs to sleep for certain time
 * ROLLBACK:
 *      Agent needs to rollback
 * STOP:
 *      Agent needs to shutdown service
 */
public enum OpCode {
    NOOP,
    DEPLOY,
    RESTART,
    DELETE,
    WAIT,
    ROLLBACK,
    STOP
}
