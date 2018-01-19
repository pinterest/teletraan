package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;


/**
 * AlertAction is the base class for an action for an alert
 */
public abstract class AlertAction {

  public abstract Object perform(AlertContext context,
                                 EnvironBean environ,
                                 DeployBean lastDeploy,
                                 int actionWindowInSeconds,
                                 String operator) throws Exception;
}
