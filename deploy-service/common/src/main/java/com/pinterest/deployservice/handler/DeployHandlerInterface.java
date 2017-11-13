package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.bean.EnvironBean;

public interface DeployHandlerInterface {
   String rollback(EnvironBean envBean, String toDeployId, String description, String operator) throws Exception;
}
