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

import java.util.ArrayList;
import java.util.List;

public class SetClause {
    private StringBuilder clauseSB;
    private List<Object> values;

    public SetClause() {
        clauseSB = new StringBuilder();
        values = new ArrayList<Object>();
    }

    public String getClause() {
        clauseSB.setLength(clauseSB.length() - 1);
        return clauseSB.toString();
    }

    public Object[] getValueArray() {
        return values.toArray();
    }

    // Add any extra value, usually key, needed by jdbc
    public void addValue(Object value) {
        values.add(value);
    }

    // Add bean name and value, keep them in name and value lists separately
    public void addColumn(String column, Object value) {
        if (value == null) {
            return;
        }
        clauseSB.append(column);
        clauseSB.append("=?,");
        if (value instanceof Enum<?>) {
            values.add(value.toString());
        } else {
            values.add(value);
        }
    }
}
