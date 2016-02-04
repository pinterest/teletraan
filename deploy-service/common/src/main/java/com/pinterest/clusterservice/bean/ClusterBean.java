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
package com.pinterest.clusterservice.bean;

import com.pinterest.deployservice.bean.SetClause;
import com.pinterest.deployservice.bean.Updatable;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Keep the bean and table in sync
 * <p/>
 * CREATE TABLE IF NOT EXISTS clusters (
 * cluster_name         VARCHAR(128)    NOT NULL,
 * capacity             INT             NOT NULL DEFAULT 0,
 * base_image_id        VARCHAR(22),
 * host_type_id         VARCHAR(22),
 * security_zone_id     VARCHAR(22),
 * placement_id         VARCHAR(22),
 * provider             VARCHAR(64),
 * assign_public_ip     TINYINT(1)      DEFAULT 0,
 * last_update          BIGINT(20)      NOT NULL,
 * PRIMARY KEY (cluster_name)
 );
 * );
 */

public class ClusterBean implements Updatable {

    private String cluster_name;
    private Integer capacity;
    private String base_image_id;
    private String host_type_id;
    private String security_zone_id;
    private String placement_id;
    private String provider;
    private Boolean assign_public_ip;
    private Long last_update;

    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getBase_image_id() {
        return base_image_id;
    }

    public void setBase_image_id(String base_image_id) {
        this.base_image_id = base_image_id;
    }

    public String getHost_type_id() {
        return host_type_id;
    }

    public void setHost_type_id(String host_type_id) {
        this.host_type_id = host_type_id;
    }

    public String getSecurity_zone_id() {
        return security_zone_id;
    }

    public void setSecurity_zone_id(String security_zone_id) {
        this.security_zone_id = security_zone_id;
    }

    public String getPlacement_id() {
        return placement_id;
    }

    public void setPlacement_id(String placement_id) {
        this.placement_id = placement_id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Boolean getAssign_public_ip() {
        return assign_public_ip;
    }

    public void setAssign_public_ip(Boolean assign_public_ip) {
        this.assign_public_ip = assign_public_ip;
    }

    public Long getLast_update() {
        return last_update;
    }

    public void setLast_update(Long last_update) {
        this.last_update = last_update;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("cluster_name", cluster_name);
        clause.addColumn("capacity", capacity);
        clause.addColumn("base_image_id", base_image_id);
        clause.addColumn("host_type_id", host_type_id);
        clause.addColumn("security_zone_id", security_zone_id);
        clause.addColumn("placement_id", placement_id);
        clause.addColumn("provider", provider);
        clause.addColumn("assign_public_ip", assign_public_ip);
        clause.addColumn("last_update", last_update);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
