package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.bean.ExternalAlert;

public abstract class ExternalAlertFactory {
  public abstract ExternalAlert getAlert(String webhookBody);
}
