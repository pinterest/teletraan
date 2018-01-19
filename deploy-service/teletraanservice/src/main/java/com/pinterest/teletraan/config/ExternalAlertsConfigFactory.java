package com.pinterest.teletraan.config;

import com.pinterest.deployservice.alerts.ExternalAlertFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import javax.validation.constraints.NotNull;

public class ExternalAlertsConfigFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ExternalAlertsConfigFactory.class);

  @NotNull
  @JsonProperty
  private String factory;

  public String getFactory() {
    return factory;
  }

  public void setFactory(String factory) {
    this.factory = factory;
  }

  public ExternalAlertFactory createExternalAlertFactory() throws Exception{

    LOG.info("Create alert factory for {}", factory);
    Class<?> factoryClass = Class.forName(factory);
    Constructor<?> ctor = factoryClass.getConstructor();
    return   (ExternalAlertFactory)ctor.newInstance();
  }
}
