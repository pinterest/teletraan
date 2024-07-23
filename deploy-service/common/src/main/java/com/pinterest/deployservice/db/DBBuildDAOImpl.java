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

import com.google.common.base.Optional;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.dao.BuildDAO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Interval;

public class DBBuildDAOImpl implements BuildDAO {

    private static final int DEFAULT_SIZE = 100;

    private static final String INSERT_BUILD_TEMPLATE = "INSERT INTO builds SET %s";
    private static final String DELETE_BUILD = "DELETE FROM builds WHERE build_id=?";
    private static final String GET_BUILD_BY_ID = "SELECT * FROM builds WHERE build_id=?";
    private static final String GET_BUILDS_BY_COMMIT_7 =
        "SELECT * FROM builds WHERE scm_commit_7=? ORDER BY publish_date DESC LIMIT ?,?";
    private static final String GET_BUILDS_BY_COMMIT_7_AND_NAME =
        "SELECT * FROM builds WHERE scm_commit_7=? AND build_name=? ORDER BY publish_date DESC LIMIT ?,?";
    private static final String GET_LATEST_BUILD_BY_NAME =
        "SELECT * FROM builds WHERE build_name=? ORDER BY publish_date DESC LIMIT 1";
    private static final String
        GET_LATEST_BUILD_BY_NAME_2 =
        "SELECT * FROM builds WHERE build_name=? AND scm_branch=? ORDER BY publish_date DESC "
            + "LIMIT 1";
    private static final String GET_BUILDS_BY_NAME =
        "SELECT * FROM builds WHERE build_name=? " + "ORDER BY publish_date DESC LIMIT ?,?";
    private static final String GET_BUILDS_BY_NAME_2 =
        "SELECT * FROM builds WHERE build_name=? AND scm_branch=? "
            + "ORDER BY publish_date DESC LIMIT ?,?";
    private static final String
        GET_BUILD_NAMES =
        "SELECT DISTINCT build_name FROM builds WHERE build_name LIKE ? ORDER BY build_name ASC "
            + "LIMIT ?,?";
    private static final String GET_BRANCHES =
        "SELECT DISTINCT scm_branch FROM builds WHERE build_name=?";
    private static final String GET_BUILD_NAMES2 =
        "SELECT DISTINCT build_name FROM builds ORDER BY build_name ASC LIMIT ?,?";
    private static final String GET_BUILDS_BY_NAME_X =
        "SELECT * FROM builds WHERE build_name=? AND "
            + "publish_date<=? AND publish_date>? ORDER BY publish_date DESC LIMIT 5000";
    private static final String GET_BUILDS_BY_NAME_X_2 =
        "SELECT * FROM builds WHERE build_name=? AND scm_branch=? AND "
            + "publish_date<=? AND publish_date>? ORDER BY publish_date DESC LIMIT 5000";
    private static final String
        GET_ACCEPTED_BUILDS_TEMPLATE =
        "SELECT * FROM builds WHERE build_name=? AND publish_date>? ORDER BY publish_date DESC "
            + "LIMIT ?";
    private static final String
        GET_ACCEPTED_BUILDS_TEMPLATE2 =
        "SELECT * FROM builds WHERE build_name=? AND scm_branch=? AND publish_date>? ORDER "
            + "BY publish_date DESC LIMIT ?";
    private static final String
        GET_ACCEPTED_BUILDS_BETWEEN_TEMPLATE =
        "SELECT * FROM builds WHERE build_name=? AND publish_date>? AND publish_date<? ORDER "
            + "BY publish_date DESC LIMIT ?";
    private static final String
        GET_ACCEPTED_BUILDS_BETWEEN_TEMPLATE2 =
        "SELECT * FROM builds WHERE build_name=? AND scm_branch=? AND publish_date>? AND "
            + "publish_date<?  ORDER BY publish_date DESC LIMIT ?";

    private static final String GET_ALL_BUILD_NAMES = "SELECT DISTINCT build_name FROM builds";
    private static final String GET_TOTAL_BY_NAME =
        "SELECT COUNT(*) FROM builds WHERE build_name=?";
    private static final String GET_LIST_OF_BUILDS_BY_IDs =
        "SELECT * FROM builds where build_id IN (%s)";

    private static final String DELETE_UNUSED_BUILDS =
        "DELETE FROM builds WHERE build_name=? AND publish_date<? "
            + "AND NOT EXISTS (SELECT 1 FROM deploys WHERE deploys.build_id = builds.build_id) "
            + "ORDER BY publish_date ASC LIMIT ?";

    private static final String GET_CURRENT_BUILD_BY_GROUP_NAME = "SELECT * FROM builds WHERE build_id IN " +
        "(SELECT build_id FROM deploys WHERE deploy_id IN " +
        "(SELECT deploy_id FROM environs WHERE env_id IN" +
        " (SELECT env_id FROM groups_and_envs WHERE group_name=?)" +
        "))";


    private BasicDataSource dataSource;

    public DBBuildDAOImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insert(BuildBean buildBean) throws Exception {
        SetClause setClause = buildBean.genSetClause();
        String clause = String.format(INSERT_BUILD_TEMPLATE, setClause.getClause());
        new QueryRunner(dataSource).update(clause, setClause.getValueArray());
    }

    @Override
    public void delete(String buildId) throws Exception {
        new QueryRunner(dataSource).update(DELETE_BUILD, buildId);
    }

    @Override
    public BuildBean getById(String buildId) throws Exception {
        ResultSetHandler<BuildBean> h = new BeanHandler<>(BuildBean.class);
        return new QueryRunner(dataSource).query(GET_BUILD_BY_ID, h, buildId);
    }

    @Override
    public List<BuildBean> getByCommit7(String scmCommit7, String buildName, int pageIndex, int pageSize)
        throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        ResultSetHandler<List<BuildBean>> h = new BeanListHandler<>(BuildBean.class);
        long start = (pageIndex - 1) * pageSize;
        if (StringUtils.isNotEmpty(buildName)) {
            return run
                .query(GET_BUILDS_BY_COMMIT_7_AND_NAME, h, scmCommit7, buildName, start, pageSize);
        } else {
            return run
                .query(GET_BUILDS_BY_COMMIT_7, h, scmCommit7, start, pageSize);
        }
    }

    @Override
    public BuildBean getLatest(String buildName, String branch) throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        ResultSetHandler<BuildBean> h = new BeanHandler<>(BuildBean.class);
        if (StringUtils.isNotEmpty(branch)) {
            return run.query(GET_LATEST_BUILD_BY_NAME_2, h, buildName, branch);
        } else {
            return run.query(GET_LATEST_BUILD_BY_NAME, h, buildName);
        }
    }

    @Override
    public List<String> getBuildNames(String nameFilter, int pageIndex, int pageSize)
        throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        if (StringUtils.isNotEmpty(nameFilter)) {
            return run
                .query(GET_BUILD_NAMES,
                    SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                    String.format("%%%s%%", nameFilter), (pageIndex - 1) * pageSize, pageSize);
        } else {
            return run
                .query(GET_BUILD_NAMES2,
                    SingleResultSetHandlerFactory.<String>newListObjectHandler(),
                    (pageIndex - 1) * pageSize, pageSize);
        }
    }

    @Override
    public List<BuildBean> getByNameDate(String buildName, String branch, long before, long after)
        throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        ResultSetHandler<List<BuildBean>> h = new BeanListHandler<>(BuildBean.class);
        if (StringUtils.isNotEmpty(branch)) {
            return run.query(GET_BUILDS_BY_NAME_X_2, h, buildName, branch, before, after);
        } else {
            return run.query(GET_BUILDS_BY_NAME_X, h, buildName, before, after);
        }
    }

    @Override
    public List<BuildBean> getByName(String buildName, String branch, int pageIndex, int pageSize)
        throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        long start = (pageIndex - 1) * pageSize;
        ResultSetHandler<List<BuildBean>> h = new BeanListHandler<>(BuildBean.class);
        if (StringUtils.isNotEmpty(branch)) {
            return run.query(GET_BUILDS_BY_NAME_2, h, buildName, branch, start, pageSize);
        } else {
            return run.query(GET_BUILDS_BY_NAME, h, buildName, start, pageSize);
        }
    }

    @Override
    public List<String> getBranches(String buildName) throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        return run.query(GET_BRANCHES, SingleResultSetHandlerFactory.<String>newListObjectHandler(),
            buildName);
    }


    @Override
    public List<BuildBean> getAcceptedBuilds(String buildName, String branch, Interval interval,
                                             int limit) throws Exception {
        ResultSetHandler<List<BuildBean>> h = new BeanListHandler<>(BuildBean.class);
        if (StringUtils.isNotEmpty(branch)) {
            return new QueryRunner(dataSource).query(GET_ACCEPTED_BUILDS_BETWEEN_TEMPLATE2, h, buildName, branch,
                    interval.getStartMillis(), interval.getEndMillis(), limit);
        } else {
            return new QueryRunner(dataSource).query(GET_ACCEPTED_BUILDS_BETWEEN_TEMPLATE, h, buildName,
                    interval.getStartMillis(), interval.getEndMillis(), limit);
        }
    }

    @Override
    public List<String> getAllBuildNames() throws Exception {
        QueryRunner run = new QueryRunner(this.dataSource);
        return run
            .query(GET_ALL_BUILD_NAMES,
                SingleResultSetHandlerFactory.<String>newListObjectHandler());
    }

    @Override
    public long countBuildsByName(String buildName) throws Exception {
        Long
            n =
            new QueryRunner(dataSource)
                .query(GET_TOTAL_BY_NAME, SingleResultSetHandlerFactory.<Long>newObjectHandler(),
                    buildName);
        return n == null ? 0 : n;
    }

    @Override
    public void deleteUnusedBuilds(String buildName, long timeThreshold, long numOfBuilds)
        throws Exception {
        new QueryRunner(dataSource)
            .update(DELETE_UNUSED_BUILDS, buildName, timeThreshold, numOfBuilds);
    }

    @Override
    public List<BuildBean> getBuildsFromIds(Collection<String> ids) throws Exception {
        if (ids.isEmpty()) {
            return new ArrayList<>(); //MySQL doesn't allow IN (). So just return empty here.
        }
        ResultSetHandler<List<BuildBean>> h = new BeanListHandler<>(BuildBean.class);
        QueryRunner run = new QueryRunner(dataSource);
        return run.query(
                String.format(GET_LIST_OF_BUILDS_BY_IDs, QueryUtils.genStringPlaceholderList(ids.size())),
                h,
                ids.toArray());
    }

    @Override
    public List<BuildBean> get(String scmCommit, String buildName, String scmBranch,
                               Optional<Integer> pageIndex, Optional<Integer> pageSize, Long before,
                               Long after)
        throws Exception {

        if (!StringUtils.isEmpty(scmCommit)) {
            return this.getByCommit7(StringUtils.substring(scmCommit, 0, 7), buildName, pageIndex.or(1),
                pageSize.or(DEFAULT_SIZE));
        }

        if (!StringUtils.isEmpty(buildName)) {
            if (before != null && after != null) {
                return this.getByNameDate(buildName, scmBranch, before, after);
            } else {
                return this
                    .getByName(buildName, scmBranch, pageIndex.or(1), pageSize.or(DEFAULT_SIZE));
            }
        }

        return new ArrayList<>();
    }

    @Override
    public List<BuildBean> getCurrentBuildsByGroupName(String groupName) throws Exception {
        ResultSetHandler<List<BuildBean>> h = new BeanListHandler<>(BuildBean.class);
        return new QueryRunner(dataSource).query(GET_CURRENT_BUILD_BY_GROUP_NAME, h, groupName);
    }
}
