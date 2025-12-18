/**
 * Copyright (c) 2025 Pinterest, Inc.
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
package com.pinterest.deployservice.bean.rodimus;

import com.pinterest.deployservice.bean.ScheduledActionBean;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents Rodimus AutoScaling Scheduled Action resource as used in Rodimus API
 *
 * <p>This is related to:
 * https://github.com/pinternal/rodimus/blob/main/common/src/main/java/com/pinterest/clusterservice/bean/AsgScheduleBean.java
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder(toBuilder = true)
public class RodimusScheduledAction {
    private String clusterName;
    private String actionId;
    private String schedule;
    private int capacity;

    /**
     * Determines if two scheduled actions match on Teletraan IaC template user-available parameters
     */
    public boolean matches(RodimusScheduledAction other) {
        return Objects.equals(this.schedule, other.schedule)
                && Objects.equals(this.capacity, other.capacity);
    }

    public static RodimusScheduledAction fromScheduledActionBean(
            String clusterName, ScheduledActionBean scheduledAction) {
        return RodimusScheduledAction.builder()
                .clusterName(clusterName)
                .actionId("")
                .schedule(scheduledAction.getSchedule())
                .capacity(scheduledAction.getCapacity())
                .build();
    }
}
