package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.handler.BuildTagHandler;
import com.pinterest.deployservice.handler.DeployHandler;

import com.pinterest.deployservice.ServiceContext;


public class DefaultAlertContextBuilder implements AlertContextBuilder {

  @Override
  public AlertContext build(ServiceContext serviceContext) {
    AlertContext context = new AlertContext();
    context.setDeployHandler(new DeployHandler(serviceContext));
    context.setTagHandler(new BuildTagHandler(serviceContext));
    context.setDeployDAO(serviceContext.getDeployDAO());
    return context;
  }
}
