package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;

import org.joda.time.Interval;

import java.util.List;

public interface DeployHandlerInterface {
   String rollback(EnvironBean envBean, String toDeployId, String description, String operator) throws Exception;
   List<DeployBean> getDeployCandidates(String envId, Interval interval, int size, boolean onlyGoodBuilds) throws Exception;
}
