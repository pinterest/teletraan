/**
 * Copyright 2021 Pinterest, Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public interface KeyReader {
    /**
     * Initialize reader with key
     * @param key a String
     */
    public void init(String key);

    /**
     * @return a key string
     */
    public String getKey();
}
