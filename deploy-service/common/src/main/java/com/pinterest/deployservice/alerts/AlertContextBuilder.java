package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.ServiceContext;

public interface AlertContextBuilder {
  AlertContext build(ServiceContext serviceContext);
}
