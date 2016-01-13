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
package com.pinterest.deployservice.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Serialize and deserialize List<AlarmConfig> to and from a JSON string
 */
public class LaunchEventParser {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmDataFactory.class);
    private static final String TYPE = "Type";
    private static final String NOTIFICATION = "Notification";
    private static final String MESSAGE_BODY = "Message";
    private static final String AUTO_SCALING_GROUP = "AutoScalingGroupName";
    private static final String EC2_INSTANCE_ID = "EC2InstanceId";
    private static final String INSTANCE_LAUNCH_TIME = "StartTime";
    private static final String CAUSE = "Cause";
    private static final String TEST_NOTIFICATION = "autoscaling:TEST_NOTIFICATION";

    /**
     * Take a JSON string and convert into a list of Alarm configs.
     */
    public LaunchEvent fromJson(String payload) {
        if (StringUtils.isEmpty(payload)) {
            return null;
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonObj = (JsonObject) parser.parse(payload);
        String notifType = jsonObj.getAsJsonPrimitive(TYPE).getAsString();
        if (!notifType.equals(NOTIFICATION)) {
            return null;
        }

        //"Message" :
        // "{\"StatusCode\":\"InProgress\",
        //   \"Service\":\"AWS Auto Scaling\",
        //   \"AutoScalingGroupName\":\"deploy-agent-test\",
        //   \"Description\":\"Launching a new EC2 instance: i-18d9deb0\",
        //   \"ActivityId\":\"6f8e26a4-2c1b-4fe7-92bd-9fcc12685562\",
        //   \"Event\":\"autoscaling:EC2_INSTANCE_LAUNCH\",
        //   \"Details\":{\"Availability Zone\":\"us-east-1e\",\"Subnet ID\":\"subnet-a5d9748e\"},
        //   \"AutoScalingGroupARN\":\"arn:aws:autoscaling:us-east-1:998131032990:autoScalingGroup:f91bd033-4e39-4b03-9c00-1410bbe05fd8:autoScalingGroupName/deploy-agent-test\",
        //   \"Progress\":50,
        //   \"Time\":\"2015-08-01T03:47:11.215Z\",
        //   \"AccountId\":\"998131032990\",
        //   \"RequestId\":\"6f8e26a4-2c1b-4fe7-92bd-9fcc12685562\",
        //   \"StatusMessage\":\"\",
        //   \"EndTime\":\"2015-08-01T03:47:11.214Z\",
        //   \"EC2InstanceId\":\"i-18d9deb0\",
        //   \"StartTime\":\"2015-08-01T03:46:08.333Z\",
        //   \"Cause\":\"At 2015-08-01T03:46:04Z a user request update of AutoScalingGroup constraints to min: 10, max: 30, desired: 10 changing the desired capacity from 5 to 10.  At 2015-08-01T03:46:06Z an instance was started in response to a difference between desired and actual capacity, increasing the capacity from 5 to 10.\"}",

        String messageBody = jsonObj.getAsJsonPrimitive(MESSAGE_BODY).getAsString();
        JsonObject message = (JsonObject)parser.parse(messageBody);
        String eventType = message.getAsJsonPrimitive("Event").getAsString();
        if (eventType.equals(TEST_NOTIFICATION)) {
            LOG.debug("Ingore test notification message:", payload);
            return null;
        }
        String groupName = message.getAsJsonPrimitive(AUTO_SCALING_GROUP).getAsString();
        String instanceId = message.getAsJsonPrimitive(EC2_INSTANCE_ID).getAsString();
        String cause = message.getAsJsonPrimitive(CAUSE).getAsString();
        long timestamp  = getDateTime(message.getAsJsonPrimitive(INSTANCE_LAUNCH_TIME).getAsString());
        return new LaunchEvent(instanceId, groupName, eventType, cause, timestamp);
    }

    private long getDateTime(String dateString) {
        try {
            return CommonUtils.convertDateStringToMilliseconds(dateString);
        } catch (Exception ex) {
            LOG.warn(String.format("Failed to parse date string: %s", dateString));
            return 0l;
        }
    }
}
