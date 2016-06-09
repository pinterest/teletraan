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
package com.pinterest.arcee.lease;



public class QuboleClusterBean {
    private int minSize;

    private int maxSize;

    private int runningReservedInstanceCount;

    private int runningSpotInstanceCount;

    private String clusterId;


    public  void setMinSize(int minSize) { this.minSize = minSize; }

    public int getMinSize() { return minSize; }

    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public int getMaxSize() { return maxSize; }

    public void setRunningReservedInstanceCount(int runningReservedInstanceCount) {
        this.runningReservedInstanceCount = runningReservedInstanceCount; }

    public int getRunningReservedInstanceCount() { return runningReservedInstanceCount; }

    public void setRunningSpotInstanceCount(int runningSpotInstanceCount) {
        this.runningSpotInstanceCount = runningSpotInstanceCount;
    }

    public int getRunningSpotInstanceCount() { return runningSpotInstanceCount; }

    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterId() { return this.clusterId; }
}
