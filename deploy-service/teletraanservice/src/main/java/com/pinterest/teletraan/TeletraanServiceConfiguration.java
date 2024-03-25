/**
 * Copyright 2016 Pinterest, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *     http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.teletraan.config.AnonymousAuthenticationFactory;
import com.pinterest.teletraan.config.AppEventFactory;
import com.pinterest.teletraan.config.AuthenticationFactory;
import com.pinterest.teletraan.config.AuthorizationFactory;
import com.pinterest.teletraan.config.AwsFactory;
import com.pinterest.teletraan.config.BuildAllowlistFactory;
import com.pinterest.teletraan.config.ChatFactory;
import com.pinterest.teletraan.config.DataSourceFactory;
import com.pinterest.teletraan.config.DefaultChatFactory;
import com.pinterest.teletraan.config.DefaultEmailFactory;
import com.pinterest.teletraan.config.DefaultHostGroupFactory;
import com.pinterest.teletraan.config.EmailFactory;
import com.pinterest.teletraan.config.ExternalAlertsConfigFactory;
import com.pinterest.teletraan.config.HostGroupFactory;
import com.pinterest.teletraan.config.JenkinsFactory;
import com.pinterest.teletraan.config.MicrometerMetricsFactory;
import com.pinterest.teletraan.config.OpenAuthorizationFactory;
import com.pinterest.teletraan.config.RodimusFactory;
import com.pinterest.teletraan.config.SourceControlFactory;
import com.pinterest.teletraan.config.SystemFactory;
import com.pinterest.teletraan.config.WorkerConfig;

import io.dropwizard.Configuration;

public class TeletraanServiceConfiguration extends Configuration {
    @Valid
    @JsonProperty("db")
    private DataSourceFactory dataSourceFactory;

    @JsonProperty("authentication")
    @Valid
    private AuthenticationFactory authenticationFactory;

    @JsonProperty("authorization")
    @Valid
    private AuthorizationFactory authorizationFactory;

    @JsonProperty("aws")
    @Valid
    private AwsFactory awsFactory;

    @JsonProperty("defaultScmTypeName")
    @Valid
    private String defaultScmTypeName;

    @Valid
    @JsonProperty("scm")
    private List<SourceControlFactory> sourceControlConfigs;

    @Valid
    @JsonProperty("chat")
    private ChatFactory chatFactory;

    @Valid
    @JsonProperty("email")
    private EmailFactory emailFactory;

    @Valid
    @JsonProperty("hostgroup")
    private HostGroupFactory hostGroupFactory;

    @Valid
    @JsonProperty("event")
    private AppEventFactory appEventFactory;

    @Valid
    @JsonProperty("workers")
    private List<WorkerConfig> workerConfigs;

    @Valid
    @JsonProperty("system")
    private SystemFactory systemFactory;


    @Valid
    @JsonProperty("rodimus")
    private RodimusFactory rodimusFactory;

    @Valid
    @JsonProperty("jenkins")
    private JenkinsFactory jenkinsFactory;

    @Valid
    @JsonProperty("buildAllowlist")
    private BuildAllowlistFactory buildAllowlistFactory;

    @Valid
    @JsonProperty("externalAlerts")
    private ExternalAlertsConfigFactory externalAlertsConfigs;

    @Valid
    @JsonProperty("pingrequestvalidators")
    private List<String> pingRequestValidators;

    @Valid
    private MicrometerMetricsFactory metricsFactory = new MicrometerMetricsFactory();

    @Valid
    @JsonProperty("accountAllowList")
    private List<String> accountAllowList;

    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public void setDataSourceFactory(DataSourceFactory factory) {
        this.dataSourceFactory = factory;
    }

    public AuthenticationFactory getAuthenticationFactory() {
        if (authenticationFactory == null) {
            return new AnonymousAuthenticationFactory();
        }
        return authenticationFactory;
    }

    public void setAuthenticationFactory(AuthenticationFactory authenticationFactory) {
        this.authenticationFactory = authenticationFactory;
    }

    public List<SourceControlFactory> getSourceControlConfigs() {
        if (sourceControlConfigs == null) {
            return Collections.emptyList();
        }
        return sourceControlConfigs;
    }

    public void setSourceControlConfigs(List<SourceControlFactory> sourceControlConfigs) {
        this.sourceControlConfigs = sourceControlConfigs;
    }

    public AuthorizationFactory getAuthorizationFactory() {
        if (authorizationFactory == null) {
            return new OpenAuthorizationFactory();
        }
        return authorizationFactory;
    }

    public BuildAllowlistFactory getBuildAllowlistFactory() {
        if (buildAllowlistFactory == null) {
            return new BuildAllowlistFactory();
        }
        return buildAllowlistFactory;
    }

    public void setBuildAllowlistFactory(BuildAllowlistFactory buildAllowlist) {
        this.buildAllowlistFactory = buildAllowlist;
    }

    public ChatFactory getChatFactory() {
        if (chatFactory == null) {
            return new DefaultChatFactory();
        }
        return chatFactory;
    }

    public void setChatFactory(ChatFactory chatFactory) {
        this.chatFactory = chatFactory;
    }


    public EmailFactory getEmailFactory() {
        if (emailFactory == null) {
            return new DefaultEmailFactory();
        }
        return emailFactory;
    }

    public void setEmailFactory(EmailFactory emailFactory) {
        this.emailFactory = emailFactory;
    }

    public void setAuthorizationFactory(AuthorizationFactory authorizationFactory) {
        this.authorizationFactory = authorizationFactory;
    }

    public HostGroupFactory getHostGroupFactory() {
        if (hostGroupFactory == null) {
            return new DefaultHostGroupFactory();
        }
        return hostGroupFactory;
    }

    public void setHostGroupFactory(HostGroupFactory hostGroupFactory) {
        this.hostGroupFactory = hostGroupFactory;
    }

    public AppEventFactory getAppEventFactory() {
        return appEventFactory;
    }

    public void setAppEventFactory(AppEventFactory appEventFactory) {
        this.appEventFactory = appEventFactory;
    }

    public SystemFactory getSystemFactory() {
        if (systemFactory == null) {
            return new SystemFactory();
        }
        return systemFactory;
    }

    public void setSystemFactory(SystemFactory systemFactory) {
        this.systemFactory = systemFactory;
    }

    public RodimusFactory getRodimusFactory() {
        return rodimusFactory;
    }

    public void setRodimusFactory(RodimusFactory rodimusFactory) {
        this.rodimusFactory = rodimusFactory;
    }

    public JenkinsFactory getJenkinsFactory() {
        return jenkinsFactory;
    }

    public void setJenkinsFactory(JenkinsFactory jenkinsFactory) {
        this.jenkinsFactory = jenkinsFactory;
    }

    public List<WorkerConfig> getWorkerConfigs() {
        if (workerConfigs == null) {
            return Collections.emptyList();
        }
        return workerConfigs;
    }

    public void setWorkerConfigs(List<WorkerConfig> workerConfigs) {
        this.workerConfigs = workerConfigs;
    }


    public ExternalAlertsConfigFactory getExternalAlertsConfigs() {
        return externalAlertsConfigs;
    }

    public void setExternalAlertsConfigs(
        ExternalAlertsConfigFactory externalAlertsConfigs) {
        this.externalAlertsConfigs = externalAlertsConfigs;
    }


    public List<String> getPingRequestValidators() {
        return pingRequestValidators;
    }

    public void setPingRequestValidators(
        List<String> pingRequestValidators) {
        this.pingRequestValidators = pingRequestValidators;
    }

    public String getDefaultScmTypeName() {
        return defaultScmTypeName;
    }

    public void setDefaultScmTypeName(String defaultScmTypeName) {
        this.defaultScmTypeName = defaultScmTypeName;
    }

    public AwsFactory getAwsFactory() {
        return awsFactory;
    }

    public void setAwsFactory(AwsFactory awsFactory) {
        this.awsFactory = awsFactory;
    }

    @JsonProperty("metrics")
    public MicrometerMetricsFactory getMetricsFactory() {
        return metricsFactory;
    }

    @JsonProperty("metrics")
    public void setMetricsFactory(MicrometerMetricsFactory metrics) {
        this.metricsFactory = metrics;
    }

    public List<String> getAccountAllowList() {
        return accountAllowList;
    }
}
