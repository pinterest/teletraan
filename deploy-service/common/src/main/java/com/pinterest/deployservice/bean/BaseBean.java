/**
 * Copyright (c) 2024 Pinterest, Inc.
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

public class BaseBean {

    /**
     * Trims the input string to the specified size limit. If the input string's length exceeds the
     * limit, the method returns the substring from the end of the string with the specified limit.
     * Otherwise returns the original string.
     *
     * @param value the input string to be trimmed
     * @param limit the maximum length of the returned string
     * @return the trimmed string if the input string's length exceeds the limit, otherwise the
     *     original string
     */
    protected String getStringWithinSizeLimit(String value, int limit) {
        if (value != null && value.length() > limit) {
            return value.substring(value.length() - limit, value.length());
        }
        return value;
    }
}
