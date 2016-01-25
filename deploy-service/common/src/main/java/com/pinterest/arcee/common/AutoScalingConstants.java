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
package com.pinterest.arcee.common;



public class AutoScalingConstants {
    public final static String DEFAULT_INSTANCE_TYPE = "c3.2xlarge";
    public final static String DEFAULT_SECURITY_GROUP = "sg-5fb76336"; // berry_security
    public final static int DEFAULT_LAUNCH_LATENCY_THRESHOLD = 600;
    public final static String DEFAULT_IAM_ROLE = "base";
    public final static int DEFAULT_GROUP_CAPACITY = 0;

    public final static String ASG_GROW = "GROW";
    public final static String ASG_SHRINK = "SHRINK";

    public final static String LIFECYCLE_ACTION_CONTINUE = "CONTINUE";
    public final static String LIFECYCLE_ACTION_ABANDON = "ABANDON";
    public final static long LIFECYCLE_TIMEOUT = 600;
}
