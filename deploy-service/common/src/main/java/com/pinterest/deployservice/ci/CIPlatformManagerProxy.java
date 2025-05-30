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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CIPlatformManagerProxy {
    private static final Logger LOG = LoggerFactory.getLogger(CIPlatformManagerProxy.class);
    Map<String, CIPlatformManager> managers;
    private List<String> validCIs;

    public CIPlatformManagerProxy(Map<String, CIPlatformManager> managers) {
        this.managers = managers;
        validCIs = new ArrayList<String>(this.managers.keySet());
    }

    private CIPlatformManager getCIPlatformManager(String ciType) throws Exception {
        CIPlatformManager manager = this.managers.get(ciType);
        if (manager == null) {
            LOG.error("Unsupported CI type: {}", ciType);
            throw new Exception("Unsupported CI type: " + ciType);
        }
        return manager;
    }

    public List<String> getCIs() throws Exception {
        return validCIs;
    }

    public String startBuild(String pipelineName, String buildParams, String ciType)
            throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        String buildID = "";
        if (manager != null && jobExists(manager.getTypeName(), pipelineName)) {
            try {
                buildID = manager.startBuild(pipelineName, buildParams);
                return buildID;
            } catch (IOException e) {
                LOG.error("Unable to start new job (hotfix-job) for {}", manager.getTypeName());
                throw new IOException(
                        "Unable to start new job (hotfix-job) for " + manager.getTypeName());
            }
        } else {
            LOG.error(
                    "Unable to get CIPlatformManager for {} OR the job {} doesn't exist",
                    ciType,
                    pipelineName);
            LOG.error("return empty buildID: {}", buildID);
            return buildID;
        }
    }

    public String getCIPlatformBaseUrl(String ciType) throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        if (manager == null) {
            throw new Exception("Unsupported CI type: " + ciType);
        }
        return manager.getCIPlatformBaseUrl();
    }

    public BaseCIPlatformBuild getBuild(String ciType, String pipelineName, String buildId)
            throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        if (manager == null) {
            throw new Exception("Unsupported CI type: " + ciType);
        }
        return manager.getBuild(pipelineName, buildId);
    }

    public boolean jobExists(String ciType, String jobName) throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        if (manager == null) {
            throw new Exception("Unsupported CI type: " + ciType);
        }
        return manager.jobExist(jobName);
    }

    public Object getCIPlatform(String ciType) throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        if (manager == null) {
            throw new Exception("Unsupported CI type: " + ciType);
        }
        return manager;
    }
}
