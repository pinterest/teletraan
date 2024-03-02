/**
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.alerts.ExternalAlertFactory;
import com.pinterest.teletraan.config.AuthorizationFactory;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;

public class TeletraanServiceContext extends ServiceContext {

  private int maxDaysToKeep;
  private int maxBuildsToKeep;
  private ExternalAlertFactory externalAlertsFactory;
  private AuthorizationFactory authorizationFactory;
  private AuthZResourceExtractor.Factory authZResourceExtractorFactory;

  public ExternalAlertFactory getExternalAlertsFactory() {
    return externalAlertsFactory;
  }

  public void setExternalAlertsFactory(
      ExternalAlertFactory externalAlertsFactory) {
    this.externalAlertsFactory = externalAlertsFactory;
  }

  public int getMaxDaysToKeep() {
    return maxDaysToKeep;
  }

  public void setMaxDaysToKeep(int maxDaysToKeep) {
    this.maxDaysToKeep = maxDaysToKeep;
  }

  public int getMaxBuildsToKeep() {
    return maxBuildsToKeep;
  }

  public void setMaxBuildsToKeep(int maxBuildsToKeep) {
    this.maxBuildsToKeep = maxBuildsToKeep;
  }

  public void setAuthorizationFactory(AuthorizationFactory authorizationFactory) {
    this.authorizationFactory = authorizationFactory;
  }

  public AuthorizationFactory getAuthorizationFactory() {
    return authorizationFactory;
  }

  public AuthZResourceExtractor.Factory getAuthZResourceExtractorFactory() {
    return authZResourceExtractorFactory;
  }

  public void setAuthZResourceExtractorFactory(
      AuthZResourceExtractor.Factory authZResourceExtractorFactory) {
    this.authZResourceExtractorFactory = authZResourceExtractorFactory;
  }
}
