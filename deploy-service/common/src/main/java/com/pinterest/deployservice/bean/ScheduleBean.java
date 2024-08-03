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
package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ScheduleBean implements Updatable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("totalSessions")
    private Integer total_sessions;

    @JsonProperty("cooldownTimes")
    private String cooldown_times;

    @JsonProperty("hostNumbers")
    private String host_numbers;

    @JsonProperty("currentSession")
    private Integer current_session;

    @JsonProperty("state")
    private ScheduleState state;

    @JsonProperty("stateStartTime")
    private Long state_start_time;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getTotal_sessions() {
        return total_sessions;
    }

    public void setTotal_sessions(Integer total_sessions) {
        this.total_sessions = total_sessions;
    }

    public String getCooldown_times() {
        return cooldown_times;
    }

    public void setCooldown_times(String cooldown_times) {
        this.cooldown_times = cooldown_times;
    }

    public String getHost_numbers() {
        return host_numbers;
    }

    public void setHost_numbers(String host_numbers) {
        this.host_numbers = host_numbers;
    }

    public Integer getCurrent_session() {
        return current_session;
    }

    public void setCurrent_session(Integer current_session) {
        // Do validation here, user input may mess it up. If we get wrong data, UI will crash

        if (this.getTotal_sessions() != null && current_session > this.getTotal_sessions()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Current session %s cannot be bigger than total_session %s ",
                            current_session, this.getTotal_sessions()));
        }
        this.current_session = current_session;
    }

    public ScheduleState getState() {
        return state;
    }

    public void setState(ScheduleState state) {
        this.state = state;
    }

    public Long getState_start_time() {
        return state_start_time;
    }

    public void setState_start_time(Long state_start_time) {
        this.state_start_time = state_start_time;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("id", id);
        clause.addColumn("total_sessions", total_sessions);
        clause.addColumn("cooldown_times", cooldown_times);
        clause.addColumn("host_numbers", host_numbers);
        clause.addColumn("current_session", current_session);
        clause.addColumn("state", state);
        clause.addColumn("state_start_time", state_start_time);
        return clause;
    }

    public static final String UPDATE_CLAUSE =
            "id=VALUES(id),"
                    + "total_sessions=VALUES(total_sessions),"
                    + "cooldown_times=VALUES(cooldown_times),"
                    + "host_numbers=VALUES(host_numbers),"
                    + "current_session=VALUES(current_session),"
                    + "state=VALUES(state),"
                    + "state_start_time=VALUES(state_start_time)";

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
