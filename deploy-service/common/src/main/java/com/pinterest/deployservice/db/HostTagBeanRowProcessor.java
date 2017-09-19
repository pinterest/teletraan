package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.HostTagInfo;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class HostTagBeanRowProcessor extends BasicRowProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(HostTagBeanRowProcessor.class);

    public HostTagBeanRowProcessor() {
    }

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
        return hostTagInfo;
    }
}
