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

public enum HotfixState {
    /** Hotfix created */
    INITIAL,
    /** Cherry-picking commits */
    PUSHING,
    /** Building hotfix build */
    BUILDING,
    /** Hotfix build created successfully */
    SUCCEEDED,
    /** Hotfix build failed to create */
    FAILED,
    /** Hotfix build was aborted */
    ABORTED
}
