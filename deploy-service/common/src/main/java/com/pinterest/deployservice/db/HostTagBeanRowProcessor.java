/**
 * Copyright (c) 2017 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.HostTagInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTagBeanRowProcessor extends BasicRowProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(HostTagBeanRowProcessor.class);

    public HostTagBeanRowProcessor() {}

    public HostTagBeanRowProcessor(BeanProcessor convert) {
        super(convert);
    }

    @Override
    public List toBeanList(ResultSet rs, Class clazz) {
        try {
            List newList = new LinkedList();
            while (rs.next()) {
                newList.add(toBean(rs, clazz));
            }
            return newList;
        } catch (SQLException ex) {
            LOG.error("Failed to convert to bean list: ", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object toBean(ResultSet rs, Class type) throws SQLException {
        HostTagInfo hostTagInfo = new HostTagInfo();
        hostTagInfo.setHostId(rs.getString("host_id"));
        hostTagInfo.setHostName(rs.getString("host_name"));
        hostTagInfo.setTagValue(rs.getString("tag_value"));
        hostTagInfo.setTagName(rs.getString("tag_name"));
        return hostTagInfo;
    }
}
