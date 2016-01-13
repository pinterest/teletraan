/**
 * Copyright 2016 Pinterest, Inc.
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
package com.pinterest.arcee.db;


import com.pinterest.arcee.bean.ImageBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.arcee.dao.ImageDAO;
import com.pinterest.deployservice.db.SingleResultSetHandlerFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class DBImageDAOImpl implements ImageDAO {
    private static final String INSERT_AMI_TEMPLATE =
            "INSERT INTO images SET %s ON DUPLICATE KEY UPDATE %s";
    private static final String DELETE_AMI =
            "DELETE FROM images WHERE id=?";
    private static final String GET_AMI_BY_ID =
            "SELECT * FROM images WHERE id=?";
    private static final String GET_AMI_BY_APPNAME =
            "SELECT * FROM images WHERE app_name=? ORDER BY publish_date DESC LIMIT ?,?";
    private static final String GET_ALL_AMI =
            "SELECT * FROM images ORDER BY publish_date DESC LIMIT ?,?";
    private static final String GET_ALL_APPNAME =
            "SELECT DISTINCT(app_name) FROM images";

    private BasicDataSource dataSource;

    public DBImageDAOImpl(BasicDataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void insertOrUpdate(ImageBean amiBean) throws Exception {
        SetClause setClause = amiBean.genSetClause();
        String clause = String.format(INSERT_AMI_TEMPLATE, setClause.getClause(), ImageBean.UPDATE_CLAUSE);
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public ImageBean getById(String amiId) throws Exception {
        ResultSetHandler<ImageBean> h = new BeanHandler<>(ImageBean.class);
        return new QueryRunner(dataSource).query(GET_AMI_BY_ID, h, amiId);
    }

    @Override
    public List<ImageBean> getImages(String appName, int pageIndex, int pageSize) throws Exception {
        QueryRunner run = new QueryRunner(dataSource);
        ResultSetHandler<List<ImageBean>> h = new BeanListHandler<>(ImageBean.class);
        if (StringUtils.isNotEmpty(appName)) {
            return run.query(GET_AMI_BY_APPNAME, h, appName, (pageIndex - 1) * pageSize, pageSize);
        } else {
            return run.query(GET_ALL_AMI, h, (pageIndex - 1) * pageSize, pageSize);
        }
    }

    @Override
    public void delete(String amiId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_AMI, amiId);
    }

    @Override
    public List<String> getAppNames() throws Exception {
        return new QueryRunner(dataSource).query(GET_ALL_APPNAME, SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }
}
