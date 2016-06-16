/*
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
package com.pinterest.arcee.bean;

import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

/**
CREATE TABLE IF NOT EXISTS group_mappings (
     asg_group_name    VARCHAR(64) NOT NULL,
     deploy_group_name VARCHAR(64) NOT NULL,
     PRIMARY KEY (asg_group_name)
 )
 **/
public class GroupMappingBean implements Updatable {
    private String asg_group_name;
    private String cluster_name;

    public String getAsg_group_name() {
        return asg_group_name;
    }

    public void setAsg_group_name(String asg_group_name) {
        this.asg_group_name = asg_group_name;
    }

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("asg_group_name", asg_group_name);
        clause.addColumn("cluster_name", cluster_name);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
        "asg_group_name=VALUES(asg_group_name)," +
        "cluster_name=VALUES(cluster_name)";
}
