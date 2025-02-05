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

public enum AcceptanceStatus {
    /** Deploy has not completed yet */
    PENDING_DEPLOY,
    /** Deploy is waiting to be tested */
    OUTSTANDING,
    /** Deploy is being tested */
    PENDING_ACCEPT,
    /** Deploy is accepted */
    ACCEPTED,
    /** Deploy is rejected */
    REJECTED,
    /** Deploy becomes inactive before accepted or rejected */
    TERMINATED
}
