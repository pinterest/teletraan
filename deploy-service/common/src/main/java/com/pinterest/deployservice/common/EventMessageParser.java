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
/**
 * Lifecycle message
{ "Messages": [
  {"Body": "
    {"AutoScalingGroupName":"exampleAutoScalingGroup",
    "Service":"AWS Auto Scaling",
    "Time":"2015-01-07T19:13:22.375Z",
    "AccountId":"356438515751",
    "LifecycleTransition":"autoscaling:EC2_INSTANCE_TERMINATING",
    "RequestId":"876eac1c-2aaa-407d-98d7-ce9afe597663",
    "LifecycleActionToken":"4889fcc7-adc6-43ff-a415-46240e2f57dc",
    "EC2InstanceId":"i-883a3a42",
    "LifecycleHookName":"exmpale-hook-name"
    }",
    "ReceiptHandle": "AQEBAjam9pe3ZxzD+w3A==",
    "MD5OfBody": "d872dc653bcd5d1cc981b2eae64d3827",
    "MessageId": "b3308afb-dad3-4eef-abb9-1d99aa9dd50f"
}]}
 */

/**
 * Autoscaling event notification
"Message" :
"{"StatusCode":"InProgress",
  "Service":"AWS Auto Scaling",
  "AutoScalingGroupName":"sample-group",
  "Description":"Launching a new EC2 instance: i-12345678",
  "ActivityId":"11111111",
  "Event":"autoscaling:EC2_INSTANCE_LAUNCH",
  "Details": {
    "Availability Zone":"us-region",
    "Subnet ID":"subnet-11111111"},
    "AutoScalingGroupARN":"arn:aws:sample",
    "Progress":50,
    "Time":"2015-08-01T03:47:11.215Z",
    "AccountId":"11111111",
    "RequestId":"11111111",
    StatusMessage":"",
    "EndTime\":"2015-08-01T03:47:11.214Z",
    "EC2InstanceId":"i-11111111",
    "StartTime":"2015-08-01T03:46:08.333Z",
    "Cause":"At 2015-08-01T03:46:04Z a user request update of AutoScalingGroup constraints to min: 10, max: 30,
    desired: 10 changing the desired capacity from 5 to 10.  At 2015-08-01T03:46:06Z an instance was started in response
    to a difference between desired and actual capacity, increasing the capacity from 5 to 10."}",
 */
public class EventMessageParser {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmDataFactory.class);
    private static final String TYPE = "Type";
    private static final String NOTIFICATION = "Notification";
    private static final String MESSAGE_BODY = "Message";
    private static final String AUTO_SCALING_GROUP = "AutoScalingGroupName";
    private static final String EC2_INSTANCE_ID = "EC2InstanceId";
    private static final String INSTANCE_LAUNCH_TIME = "StartTime";
    private static final String CAUSE = "Cause";
    private static final String TEST_NOTIFICATION = "autoscaling:TEST_NOTIFICATION";
    private static final String LIFECYCLE_TOKEN = "LifecycleActionToken";
    private static final String LIFECYCLE_HOOK = "LifecycleHookName";
    private static final String LIFECYCLE_ACTION_TYPE = "LifecycleTransition";

    /**
     * Take a JSON string and convert into a list of Alarm configs.
     */
    public EventMessage fromJson(String payload) {
        if (StringUtils.isEmpty(payload)) {
            return null;
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonObj = (JsonObject) parser.parse(payload);
        String notifType = jsonObj.getAsJsonPrimitive(TYPE).getAsString();
        if (!notifType.equals(NOTIFICATION)) {
            return null;
        }

        String messageBody = jsonObj.getAsJsonPrimitive(MESSAGE_BODY).getAsString();
        JsonObject message = (JsonObject)parser.parse(messageBody);
        String groupName = message.getAsJsonPrimitive(AUTO_SCALING_GROUP).getAsString();

        // Lifecycle event parsing
        if (message.getAsJsonPrimitive(LIFECYCLE_ACTION_TYPE) != null) {
            String lifecycleActionType = message.getAsJsonPrimitive(LIFECYCLE_ACTION_TYPE).getAsString();
            LOG.debug(String.format("Lifecycle %s notification message", lifecycleActionType));
            String instanceId = message.getAsJsonPrimitive(EC2_INSTANCE_ID).getAsString();
            String lifecycleToken = message.getAsJsonPrimitive(LIFECYCLE_TOKEN).getAsString();
            long timestamp  = getDateTime(message.getAsJsonPrimitive("Time").getAsString());
            if (!StringUtils.isEmpty(lifecycleToken)) {
                String lifecycleHook = message.getAsJsonPrimitive(LIFECYCLE_HOOK).getAsString();
                return new EventMessage(lifecycleToken, lifecycleHook, groupName, lifecycleActionType, instanceId, timestamp);
            }
        }

        String eventType = message.getAsJsonPrimitive("Event").getAsString();
        if (eventType.equals(TEST_NOTIFICATION)) {
            LOG.debug("Ingore test notification message:", payload);
            return null;
        }

        // Autoscaling event parsing
        String instanceId = message.getAsJsonPrimitive(EC2_INSTANCE_ID).getAsString();
        String cause = message.getAsJsonPrimitive(CAUSE).getAsString();
        long timestamp  = getDateTime(message.getAsJsonPrimitive(INSTANCE_LAUNCH_TIME).getAsString());
        return new EventMessage(instanceId, groupName, eventType, cause, timestamp);
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
