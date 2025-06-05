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
package com.pinterest.deployservice.ci;

public abstract class BaseCIPlatformBuild implements CIPlatformBuild {
    private String buildID;
    private String status;
    private long startTimestamp;
    private long duration;

    public BaseCIPlatformBuild(String buildID, String status, long startTimestamp, long duration) {
        this.buildID = buildID;
        this.status = status;
        this.startTimestamp = startTimestamp;
        this.duration = duration;
    }

    @Override
    public String getBuildUUID() {
        return this.buildID;
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }

    public long getDuration() {
        return this.duration;
    }
}
