package com.pinterest.deployservice.ci;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CIPlatformManagerProxy {
    private static final Logger LOG = LoggerFactory.getLogger(CIPlatformManagerProxy.class);
    Map<String, CIPlatformManager> managers;
    private List<String> validCIs;

    public CIPlatformManagerProxy(
            Map<String, CIPlatformManager> managers) {
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

    public void startBuild(
            String pipelineName,
            String buildParams)
            throws Exception {
        ArrayList<String> buildIDs = new ArrayList<String>();
        for (CIPlatformManager manager : this.managers.values()) {
            if (manager != null && jobExists(manager.getTypeName(), pipelineName)) {
                String buildID = manager.startBuild(pipelineName, buildParams);
                if (buildID != null) {
                    buildIDs.add(buildID);
                }
            }
            else {
                LOG.error("Unable to start new job (hotfix-job) for {}", manager.getTypeName());
                throw new Exception("Unable to start new job (hotfix-job) for " + manager.getTypeName());
            }
        }
    }

    public String startBuild(
            String pipelineName,
            String buildParams,
            String ciType)
            throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        if (manager != null && jobExists(manager.getTypeName(), pipelineName)) {
            String buildID = manager.startBuild(pipelineName, buildParams);
            return buildID;
        } else {
            LOG.error("Unable to start new job (hotfix-job) for {}", manager.getTypeName());
            throw new Exception("Unable to start new job (hotfix-job) for " + manager.getTypeName());
        }
    }

    public Object getBuild(
            String ciType,
            String pipelineName,
            String buildId)
            throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        if (manager == null) {
            throw new Exception("Unsupported CI type: " + ciType);
        }
        return manager.getBuild(pipelineName, buildId);
    }

    public boolean jobExists(
            String ciType,
            String jobName)
            throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        if (manager == null) {
            throw new Exception("Unsupported CI type: " + ciType);
        }
        return manager.jobExist(jobName);
    }

    public Object getCIPlatform(String ciType)
            throws Exception {
        CIPlatformManager manager = getCIPlatformManager(ciType);
        if (manager == null) {
            throw new Exception("Unsupported CI type: " + ciType);
        }
        return manager;
    }
}
