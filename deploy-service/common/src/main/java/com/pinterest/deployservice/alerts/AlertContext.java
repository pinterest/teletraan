package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.handler.DeployHandlerInterface;
import com.pinterest.deployservice.handler.TagHandler;

public class AlertContext {
  private DeployHandlerInterface deployHandler;
  private TagHandler tagHandler;
  private DeployDAO deployDAO;

  public DeployDAO getDeployDAO() {
    return deployDAO;
  }

  public void setDeployDAO(DeployDAO deployDAO) {
    this.deployDAO = deployDAO;
  }

  public DeployHandlerInterface getDeployHandler() {
    return deployHandler;
  }

  public void setDeployHandler(DeployHandlerInterface deployHandler) {
    this.deployHandler = deployHandler;
  }

  public TagHandler getTagHandler() {
    return tagHandler;
  }

  public void setTagHandler(TagHandler tagHandler) {
    this.tagHandler = tagHandler;
  }
}
