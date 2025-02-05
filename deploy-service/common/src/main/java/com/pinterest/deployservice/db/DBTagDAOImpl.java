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

import com.google.common.base.Preconditions;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.dao.TagDAO;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

public class DBTagDAOImpl implements TagDAO {

    private static final String INSERT_TAG_TEMPLATE = "INSERT INTO tags SET %s";

    private static final String DELETE_TAG_TEMPLATE = "DELETE FROM tags WHERE id=?";

    private static final String GET_TAG_BY_ID_TEMPLATE = "SELECT * FROM tags WHERE id =?";

    private static final String GET_TAG_BY_TARGET_ID_TEMPLATE =
            "SELECT * FROM tags WHERE target_id=? ORDER BY created_date DESC";

    private static final String GET_TAG_BY_VALUE = "SELECT * FROM tags WHERE value=?";

    private static final String GET_TAG_BY_TARGET_ID_AND_TYPE_TEMPLATE =
            "SELECT * FROM tags WHERE target_id=? AND "
                    + " target_type=? ORDER BY created_date DESC";

    private static final String GET_LATEST_TAG_BY_TARGET_ID_AND_TYPE_TEMPLATE =
            "SELECT * FROM tags WHERE target_id=? AND "
                    + " target_type=? ORDER BY created_date DESC LIMIT ?";

    private static final String GET_LATEST_TAG_BY_TARGET_ID =
            "SELECT * FROM tags WHERE target_id=? ORDER BY created_date DESC LIMIT 0,1";

    private BasicDataSource basicDataSource;

    public DBTagDAOImpl(BasicDataSource source) {
        this.basicDataSource = source;
    }

    public void insert(TagBean tagInfo) throws Exception {
        SetClause setClause = tagInfo.genSetClause();
        String clause = String.format(INSERT_TAG_TEMPLATE, setClause.getClause());
        new QueryRunner(basicDataSource).update(clause, setClause.getValueArray());
    }

    public void delete(String id) throws Exception {
        new QueryRunner(basicDataSource).update(DELETE_TAG_TEMPLATE, id);
    }

    @Override
    public TagBean getById(String id) throws Exception {
        return new QueryRunner(basicDataSource)
                .query(GET_TAG_BY_ID_TEMPLATE, new BeanHandler<>(TagBean.class), id);
    }

    @Override
    public List<TagBean> getByTargetId(String target_id) throws Exception {
        ResultSetHandler<List<TagBean>> h = new BeanListHandler<TagBean>(TagBean.class);
        return new QueryRunner(basicDataSource).query(GET_TAG_BY_TARGET_ID_TEMPLATE, h, target_id);
    }

    @Override
    public List<TagBean> getByTargetIdAndType(String target_id, TagTargetType target_type)
            throws Exception {
        ResultSetHandler<List<TagBean>> h = new BeanListHandler<TagBean>(TagBean.class);
        return new QueryRunner(basicDataSource)
                .query(
                        GET_TAG_BY_TARGET_ID_AND_TYPE_TEMPLATE,
                        h,
                        target_id,
                        target_type.toString());
    }

    @Override
    public List<TagBean> getLatestByTargetIdAndType(
            String target_id, TagTargetType target_type, int size) throws Exception {
        Preconditions.checkArgument(size > 0);
        ResultSetHandler<List<TagBean>> h = new BeanListHandler<TagBean>(TagBean.class);
        return new QueryRunner(basicDataSource)
                .query(
                        GET_LATEST_TAG_BY_TARGET_ID_AND_TYPE_TEMPLATE,
                        h,
                        target_id,
                        target_type.toString(),
                        size);
    }

    @Override
    public List<TagBean> getByValue(TagValue value) throws Exception {
        ResultSetHandler<List<TagBean>> h = new BeanListHandler<TagBean>(TagBean.class);
        return new QueryRunner(basicDataSource).query(GET_TAG_BY_VALUE, h, value.toString());
    }

    @Override
    public TagBean getLatestByTargetId(String targetId) throws Exception {
        return new QueryRunner(basicDataSource)
                .query(GET_LATEST_TAG_BY_TARGET_ID, new BeanHandler<>(TagBean.class), targetId);
    }
}
