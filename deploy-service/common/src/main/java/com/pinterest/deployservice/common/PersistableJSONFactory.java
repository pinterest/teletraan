/**
 * Copyright (c) 2016 Pinterest, Inc.
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
package com.pinterest.deployservice.common;

/**
 * An implementation of PersistableJSONFactory is capable of serialize and deserialize arbitrary
 * object E to and from JSON string. This JSON string will be stored in datas table as BLOB
 */
public interface PersistableJSONFactory<E> {
    /**
     * Take an arbitrary object and convert into a JSON string
     *
     * @param e an Object
     * @return a JSON string
     * @throws Exception
     */
    String toJson(E e) throws Exception;

    /**
     * Parse a JSON string previously converted from an object back to this object
     *
     * @param payload a JSON string
     * @return an Object
     * @throws Exception
     */
    E fromJson(String payload) throws Exception;
}
