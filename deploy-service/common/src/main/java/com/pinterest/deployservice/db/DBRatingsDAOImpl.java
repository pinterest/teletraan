/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.RatingBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.RatingDAO;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

/** Implementation for Ratings DAO */
public class DBRatingsDAOImpl implements RatingDAO {

    private static final String GET_RATINGS =
            "SELECT * FROM user_ratings ORDER BY timestamp DESC LIMIT ?,?";
    private static final String INSERT_RATING_TEMPLATE = "INSERT INTO user_ratings SET %s";
    private static final String GET_USER_RATINGS =
            "SELECT * FROM user_ratings where author=? ORDER BY timestamp";
    private static final String DELETE_RATING = "DELETE FROM user_ratings WHERE rating_id=?";

    private BasicDataSource dataSource;

    public DBRatingsDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(RatingBean bean) throws Exception {
        SetClause setClause = bean.genSetClause();
        String clause = String.format(INSERT_RATING_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String id) throws Exception {
        new QueryRunner(dataSource).update(DELETE_RATING, id);
    }

    @Override
    public List<RatingBean> getRatingsInfos(int pageIndex, int pageSize) throws Exception {
        ResultSetHandler<List<RatingBean>> h = new BeanListHandler<RatingBean>(RatingBean.class);
        QueryRunner run = new QueryRunner(this.dataSource);
        return run.query(GET_RATINGS, h, (pageIndex - 1) * pageSize, pageSize);
    }

    @Override
    public List<RatingBean> getRatingsByAuthor(String author) throws Exception {
        ResultSetHandler<List<RatingBean>> h = new BeanListHandler<RatingBean>(RatingBean.class);
        QueryRunner run = new QueryRunner(this.dataSource);
        return run.query(GET_USER_RATINGS, h, author);
    }
}
