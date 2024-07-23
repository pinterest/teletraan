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
package com.pinterest.deployservice.db;

import com.pinterest.deployservice.bean.DeployFilterBean;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DeployQueryFilter {
    private static final int DEFAULT_PAGE_INDEX = 1;
    private static final int DEFAULT_PAGE_SIZE = 30;

    private String whereClause;
    private List<Object> values;
    private DeployFilterBean filter;

    public DeployQueryFilter(DeployFilterBean filter) {
        this.filter = filter;
        if (filter.getPageIndex() == null) {
            filter.setPageIndex(DEFAULT_PAGE_INDEX);
        }
        if (filter.getPageSize() == null) {
            filter.setPageSize(DEFAULT_PAGE_SIZE);
        }
    }

    public String getWhereClause() {
        return whereClause;
    }

    public Object[] getValueArray() {
        return values.toArray();
    }

    public DeployFilterBean getFilter() {
        return this.filter;
    }

    private StringBuilder appendSubQuery(StringBuilder sb, String name, List<String> valueList) {
        if (valueList == null || valueList.isEmpty()) {
            return sb;
        }
        sb.append("(");
        for (String value : valueList) {
            sb.append(name);
            sb.append("=? OR ");
            values.add(value);
        }
        // remove the trialing OR and space
        sb.setLength(sb.length() - 3);
        sb.append(") AND ");
        return sb;
    }

    private <E extends Enum<E>> StringBuilder appendSubQueryEnum(StringBuilder sb, String name,
        List<E> valueList) {
        if (valueList == null || valueList.isEmpty()) {
            return sb;
        }
        sb.append("(");
        for (E value : valueList) {
            sb.append(name);
            sb.append("=? OR ");
            values.add(value.toString());
        }
        // remove the trialing OR and space
        sb.setLength(sb.length() - 3);
        sb.append(") AND ");
        return sb;
    }

    private StringBuilder appendSubQuery(StringBuilder sb, String name, String value) {
        if (StringUtils.isEmpty(value)) {
            return sb;
        }
        sb.append(name);
        sb.append("=? AND ");
        values.add(value);
        return sb;
    }

    public void generateClauseAndValues() {
        this.values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        sb = appendSubQuery(sb, "env_id", filter.getEnvIds());
        sb = appendSubQuery(sb, "operator", filter.getOperators());
        sb = appendSubQueryEnum(sb, "deploy_type", filter.getDeployTypes());
        sb = appendSubQueryEnum(sb, "state", filter.getDeployStates());
        sb = appendSubQueryEnum(sb, "acc_status", filter.getAcceptanceStatuss());
        sb = appendSubQuery(sb, "scm_commit", filter.getCommit());
        sb = appendSubQuery(sb, "scm_repo", filter.getRepo());
        sb = appendSubQuery(sb, "scm_branch", filter.getBranch());

        if (filter.getCommitDate() != null) {
            sb.append("commit_date>=? AND ");
            values.add(filter.getCommitDate());
        }

        if (filter.getBefore() != null) {
            sb.append("start_date<=? AND ");
            values.add(filter.getBefore());
        }

        if (filter.getAfter() != null) {
            sb.append("start_date>=? AND ");
            values.add(filter.getAfter());
        }


        if (sb.length() > 1) {
            // remove the trialing AND and space
            sb.setLength(sb.length() - 4);
            sb.insert(0, "WHERE ");
        }

        if (filter.getOldestFirst() != null && filter.getOldestFirst()) {
            sb.append("ORDER BY start_date ASC LIMIT ?,?");
        } else {
            sb.append("ORDER BY start_date DESC LIMIT ?,?");
        }

        values.add((filter.getPageIndex() - 1) * filter.getPageSize());
        values.add(filter.getPageSize());

        this.whereClause = sb.toString();
    }
}
