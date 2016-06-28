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


import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

public class SpotAutoScalingBean implements Updatable {
    @JsonProperty("cluster_name")
    private String cluster_name;

    @JsonProperty("bid_price")
    private String bid_price;

    @JsonProperty("spot_ratio")
    private Double spot_ratio;

    @JsonProperty("sensitivity_ratio")
    private Double sensitivity_ratio;

    @JsonProperty("enable_grow")
    private Boolean enable_grow;

    @JsonProperty("enable_resource_landing")
    private Boolean enable_resource_lending;

    public String getCluster_name() { return cluster_name; }

    public void setCluster_name(String cluster_name) { this.cluster_name = cluster_name; }

    public String getBid_price() { return bid_price; }

    public void setBid_price(String bid_price) { this.bid_price = bid_price; }

    public Double getSpot_ratio() { return spot_ratio; }

    public void setSpot_ratio(Double spot_ratio) { this.spot_ratio = spot_ratio; }

    public Double getSensitivity_ratio() { return sensitivity_ratio; }

    public void setSensitivity_ratio(Double sensitivity_ratio) { this.sensitivity_ratio = sensitivity_ratio; }

    public Boolean getEnable_grow() { return enable_grow; }

    public void setEnable_grow(Boolean enable_grow) { this.enable_grow = enable_grow; }

    public Boolean getEnable_resource_lending() {  return enable_resource_lending; }

    public void setEnable_resource_lending(Boolean enable_resource_lending) { this.enable_resource_lending = enable_resource_lending; }
    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("cluster_name", cluster_name);
        clause.addColumn("bid_price", bid_price);
        clause.addColumn("spot_ratio", spot_ratio);
        clause.addColumn("sensitivity_ratio", sensitivity_ratio);
        clause.addColumn("enable_grow", enable_grow);
        clause.addColumn("enable_resource_lending", enable_resource_lending);
        return clause;
    }

    public final static String UPDATE_CLAUSE =
                    "cluster_name=VALUES(cluster_name)," +
                    "bid_price=VALUES(bid_price), " +
                    "spot_ratio=VALUES(spot_ratio)," +
                    "sensitivity_ratio=VALUES(sensitivity_ratio)," +
                    "enable_grow=VALUES(enable_grow)," +
                    "enable_resource_lending=VALUES(enable_resource_lending)";
}
