package com.pinterest.deployservice.bean;

import com.pinterest.deployservice.handler.GoalAnalyst;

import java.util.List;

public class PingResult {

    private PingResponseBean responseBean;
    private List<GoalAnalyst.InstallCandidate> installCandidates;
    private List<GoalAnalyst.UninstallCandidate> uninstallCandidateList;

    public PingResult() {

    }

    public PingResponseBean getResponseBean() {
        return responseBean;
    }

    public List<GoalAnalyst.InstallCandidate> getInstallCandidates() {
        return installCandidates;
    }

    public List<GoalAnalyst.UninstallCandidate> getUninstallCandidateList() {
        return uninstallCandidateList;
    }

    public PingResult withResponseBean(PingResponseBean bean) {
        this.responseBean = bean;
        return this;
    }

    public PingResult withInstallCandidates(List<GoalAnalyst.InstallCandidate> candidates) {
        this.installCandidates = candidates;
        return this;
    }

    public PingResult withUnInstallCandidates(List<GoalAnalyst.UninstallCandidate> candidates) {
        this.uninstallCandidateList = candidates;
        return this;
    }
}
