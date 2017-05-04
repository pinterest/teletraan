package com.pinterest.deployservice.bean;

import java.util.ArrayList;
import java.util.List;

public class DeployCandidatesResponse {
    private List<PingResponseBean> candidates = new ArrayList<>();

    public List<PingResponseBean> getCandidates() {
        return candidates;
    }

}
