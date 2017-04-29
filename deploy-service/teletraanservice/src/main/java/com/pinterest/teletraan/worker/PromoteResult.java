package com.pinterest.teletraan.worker;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class PromoteResult {

  public Pair<DeployBean, EnvironBean> predDeployInfo;
  private ResultCode result;
  private String promotedBuild;

  public PromoteResult() {

  }

  public ResultCode getResult() {
    return result;
  }

  public String getPromotedBuild() {
    return promotedBuild;
  }

  public Pair<DeployBean, EnvironBean> getPredDeployInfo() {
    return predDeployInfo;
  }

  public PromoteResult withResultCode(ResultCode result) {
    this.result = result;
    return this;
  }

  public PromoteResult withBuild(String buildId) {
    this.promotedBuild = buildId;
    return this;
  }

  public PromoteResult withPredDeployBean(DeployBean deployBean, EnvironBean environBean) {
    this.predDeployInfo = new ImmutablePair<DeployBean, EnvironBean>(deployBean, environBean);
    return this;
  }

  public enum ResultCode {
    NotInScheduledTime,
    NoAvailableBuild,
    NoPredEnvironment,
    NoPredEnvironmentDeploy,
    NoCandidateWithinDelayPeriod,
    NoRegularDeployWithinDelayPeriod,
    PromoteBuild,
    PromoteDeploy
  }
}
