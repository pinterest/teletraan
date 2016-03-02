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
package com.pinterest.arcee.bean;


import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

public class SpotAutoScalingBean implements Updatable {
    private String asg_name;

    private String cluster_name;

    private String launch_config_id;

    private String bid_price;

    private Double spot_ratio;

    private Double sensitivity_ratio;

    public String getAsg_name() { return asg_name; }

    public void setAsg_name(String asg_name) { this.asg_name = asg_name; }

    public String getCluster_name() { return cluster_name; }

    public void setCluster_name(String cluster_name) { this.cluster_name = cluster_name; }

    public String getLaunch_config_id() { return launch_config_id; }

    public void setLaunch_config_id(String launch_config_id) { this.launch_config_id = launch_config_id;}

    public String getBid_price() { return bid_price; }

    public void setBid_price(String bid_price) { this.bid_price = bid_price; }

    public Double getSpot_ratio() { return spot_ratio; }

    public void setSpot_ratio(Double spot_ratio) { this.spot_ratio = spot_ratio; }

    public Double getSensitivity_ratio() { return sensitivity_ratio; }

    public void setSensitivity_ratio(Double sensitivity_ratio) { this.sensitivity_ratio = sensitivity_ratio; }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("asg_name", asg_name);
        clause.addColumn("cluster_name", cluster_name);
        clause.addColumn("launch_config_id", launch_config_id);
        clause.addColumn("bid_price", bid_price);
        clause.addColumn("spot_ratio", spot_ratio);
        clause.addColumn("sensitivity_ratio", sensitivity_ratio);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
            "asg_name=VALUES(asg_name)," +
                    "cluster_name=VALUES(cluster_name)," +
                    "launch_config_id=VALUES(launch_config_id)," +
                    "bid_price=VALUES(bid_price), " +
                    "spot_ratio=VALUES(spot_ratio)," +
                    "sensitivity_ratio=VALUES(sensitivity_ratio)";
}
