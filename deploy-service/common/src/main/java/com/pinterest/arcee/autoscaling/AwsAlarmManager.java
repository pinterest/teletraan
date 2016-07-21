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
package com.pinterest.arcee.autoscaling;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;

import java.util.*;

public class AwsAlarmManager implements AlarmManager {
    private AmazonCloudWatchClient acwClient;

    public AwsAlarmManager(AmazonCloudWatchClient client) {
        acwClient = client;
    }


    @Override
    public void enableAlarm(List<String> alarmIds, String groupName) throws Exception {
        List<String> alarmNames = new ArrayList<>();
        for (String alarmId : alarmIds) {
            alarmNames.add(getAlarmName(groupName, alarmId));
        }
        EnableAlarmActionsRequest request = new EnableAlarmActionsRequest();
        request.setAlarmNames(alarmNames);
        acwClient.enableAlarmActions(request);
    }

    @Override
    public void disableAlarm(List<String> alarmIds, String groupName) throws Exception {
        List<String> alarmNames = new ArrayList<>();
        for (String alarmId : alarmIds) {
            alarmNames.add(getAlarmName(groupName, alarmId));
        }
        DisableAlarmActionsRequest request = new DisableAlarmActionsRequest();
        request.setAlarmNames(alarmNames);
        acwClient.disableAlarmActions(request);
    }

    private String getAlarmName(String groupName, String alarmId) {
        return String.format("%s-alarm-%s", groupName, alarmId);
    }
}
