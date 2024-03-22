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

import com.pinterest.deployservice.bean.HostTagBean;
import com.pinterest.deployservice.bean.HostTagInfo;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.UpdateStatement;
import com.pinterest.deployservice.dao.HostTagDAO;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.RowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

public class DBHostTagDAOImpl implements HostTagDAO {

    private static final String INSERT_HOST_TAG_TEMPLATE =
            "INSERT INTO host_tags SET %s ON DUPLICATE KEY UPDATE %s";
    private static final String DELETE_HOST_TAG_BY_ENV_ID_AND_HOST_ID =
            "DELETE FROM host_tags WHERE env_id = ? AND host_id IN ( %s ) ";
    private static final String DELETE_HOST_TAG_BY_ENV_ID_AND_TAG_NAME =
            "DELETE FROM host_tags WHERE env_id = ? AND tag_name = ? ";
    private static final String DELETE_BY_HOST_ID = "DELETE FROM host_tags WHERE host_id = ?";
    private static final String GET_HOST_TAG_BY_HOST_ID_AND_TAG_NAME =
            "SELECT * FROM host_tags WHERE host_id = ? AND tag_name = ? ";
    private static final String GET_PREREQUISITES_TAG_VALUES_BY_HOST_ID_AND_TAG_NAME =
            "SELECT DISTINCT(tag_value) AS tag_value FROM host_tags WHERE env_id = ? AND tag_name = ? AND tag_value < ?";
    private static final String GET_HOSTS_BY_ENV_ID_AND_TAG_NAME =
            "SELECT DISTINCT(host_tags.host_id) AS host_id, host_tags.tag_value AS tag_value, host_tags.tag_name AS tag_name, agents.host_name AS host_name FROM agents "
                    + "INNER JOIN host_tags ON agents.host_id = host_tags.host_id WHERE host_tags.tag_name = ? AND agents.env_id = ?";
    private static final String GET_HOSTS_BY_ENV_ID =
            "SELECT DISTINCT(host_tags.host_id) AS host_id, host_tags.tag_value AS tag_value, host_tags.tag_name AS tag_name, hosts.host_name AS host_name FROM hosts "
                    + "INNER JOIN host_tags ON hosts.host_id = host_tags.host_id "
                    + "WHERE host_tags.env_id = ?";
    private static final String COUNT_HOSTS_BY_ENV_ID_AND_TAGS =
            "SELECT count(DISTINCT(host_id)) FROM host_tags WHERE env_id = ? AND tag_name = ? AND tag_value IN (%s) ";
    private static final String GET_ALL_BY_ENV_ID_AND_TAG_NAME =
            "SELECT * FROM host_tags WHERE env_id = ? AND tag_name = ? ";
    private static final RowProcessor ROW_PROCESSOR = new HostTagBeanRowProcessor();
    private BasicDataSource dataSource;

    public DBHostTagDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertOrUpdate(HostTagBean hostTagBean) throws Exception {
        SetClause setClause = hostTagBean.genSetClause();
        String clause =
                String.format(
                        INSERT_HOST_TAG_TEMPLATE, setClause.getClause(), HostTagBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public UpdateStatement genInsertOrUpdate(HostTagBean hostTagBean) {
        SetClause setClause = hostTagBean.genSetClause();
        String clause =
                String.format(
                        INSERT_HOST_TAG_TEMPLATE, setClause.getClause(), HostTagBean.UPDATE_CLAUSE);
        return new UpdateStatement(clause, setClause.getValueArray());
    }

    @Override
    public HostTagBean get(String hostId, String tagName) throws Exception {
        ResultSetHandler<HostTagBean> h = new BeanHandler<HostTagBean>(HostTagBean.class);
        return new QueryRunner(dataSource)
                .query(GET_HOST_TAG_BY_HOST_ID_AND_TAG_NAME, h, hostId, tagName);
    }

    @Override
    public void deleteAllByEnvId(String envId, String tagName) throws Exception {
        new QueryRunner(dataSource).update(DELETE_HOST_TAG_BY_ENV_ID_AND_TAG_NAME, envId, tagName);
    }

    @Override
    public void deleteByHostId(String hostId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_BY_HOST_ID, hostId);
    }

    @Override
    public void deleteAllByEnvIdAndHostIds(String envId, List<String> hostIds) throws Exception {
        String hostStr = QueryUtils.genStringGroupClause(hostIds);
        new QueryRunner(dataSource)
                .update(String.format(DELETE_HOST_TAG_BY_ENV_ID_AND_HOST_ID, hostStr), envId);
    }

    @Override
    public List<HostTagBean> getAllByEnvIdAndTagName(String envId, String tagName)
            throws Exception {
        ResultSetHandler<List<HostTagBean>> h = new BeanListHandler<>(HostTagBean.class);
        return new QueryRunner(dataSource).query(GET_ALL_BY_ENV_ID_AND_TAG_NAME, h, envId, tagName);
    }

    @Override
    public List<HostTagInfo> getHostsByEnvIdAndTagName(String envId, String tagName)
            throws Exception {
        ResultSetHandler<List<HostTagInfo>> h =
                new BeanListHandler<>(HostTagInfo.class, ROW_PROCESSOR);
        return new QueryRunner(dataSource)
                .query(GET_HOSTS_BY_ENV_ID_AND_TAG_NAME, h, tagName, envId);
    }

    @Override
    public List<HostTagInfo> getHostsByEnvId(String envId) throws Exception {
        ResultSetHandler<List<HostTagInfo>> h =
                new BeanListHandler<>(HostTagInfo.class, ROW_PROCESSOR);
        return new QueryRunner(dataSource).query(GET_HOSTS_BY_ENV_ID, h, envId);
    }

    @Override
    public long countHostsByEnvIdAndTags(String envId, String tagName, List<String> tagValues)
            throws Exception {
        String tagValuesStr = QueryUtils.genStringGroupClause(tagValues);
        Long n =
                new QueryRunner(dataSource)
                        .query(
                                String.format(COUNT_HOSTS_BY_ENV_ID_AND_TAGS, tagValuesStr),
                                SingleResultSetHandlerFactory.<Long>newObjectHandler(),
                                envId,
                                tagName);
        return n == null ? 0 : n;
    }

    @Override
    public List<String> getAllPrerequisiteTagValuesByEnvIdAndTagName(
            String envId, String tagName, String tagValue) throws Exception {
        return new QueryRunner(dataSource)
                .query(
                        GET_PREREQUISITES_TAG_VALUES_BY_HOST_ID_AND_TAG_NAME,
                        SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                        envId,
                        tagName,
                        tagValue);
    }
}
