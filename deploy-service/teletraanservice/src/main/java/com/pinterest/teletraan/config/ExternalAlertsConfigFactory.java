package com.pinterest.teletraan.config;

import com.pinterest.deployservice.alerts.ExternalAlertFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Constructor;
import javax.validation.constraints.NotNull;

public class ExternalAlertsConfigFactory {
  @NotNull
  @JsonProperty
  private String factory;

  public ExternalAlertFactory createExternalAlertFactory() throws Exception{

    Class<?> factoryClass = Class.forName(factory);
    Constructor<?> ctor = factoryClass.getConstructor();
    return   (ExternalAlertFactory)ctor.newInstance();
  }
}
