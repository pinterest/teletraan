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
package com.pinterest.deployservice.db;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

public class QueryUtils {
    public static String genStringGroupClause(Collection<String> names) {
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            sb.append("'");
            sb.append(name);
            sb.append("',");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String genStringPlaceholderList(int size) {
        return StringUtils.repeat("?", ",", size);
    }

    public static <E extends Enum<E>> String genEnumGroupClause(Collection<E> names) {
        StringBuilder sb = new StringBuilder();
        for (E name : names) {
            sb.append("'");
            sb.append(name.toString());
            sb.append("',");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
