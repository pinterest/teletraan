/**
 * Copyright (c) 2018-2024 Pinterest, Inc.
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
package com.pinterest.deployservice.knox;

/** Knox is an interface for Java services to interface with the Knox service. */
public interface Knox {

    /** getPrimaryKey returns the primary key for the specified keyID */
    public byte[] getPrimaryKey() throws Exception;

    /** getActiveKeys returns a list of active keys for the specified keyID */
    public byte[][] getActiveKeys() throws Exception;
}
