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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.dbutils.ResultSetHandler;

public class SingleResultSetHandlerFactory {

    public static <T> ResultSetHandler<T> newObjectHandler() {
        return new ResultSetHandler<T>() {
            @Override
            public T handle(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    return (T) resultSet.getObject(1);
                }
                return null;
            }
        };
    }

    public static <T> ResultSetHandler<List<T>> newListObjectHandler() {
        return new ResultSetHandler<List<T>>() {
            @Override
            public List<T> handle(ResultSet resultSet) throws SQLException {
                List<T> ret = new ArrayList<T>();
                while (resultSet.next()) {
                    ret.add((T) resultSet.getObject(1));
                }
                return ret;
            }
        };
    }
}
