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

import com.pinterest.teletraan.config.AnonymousAuthenticationFactory;
import com.pinterest.teletraan.config.AuthenticationFactory;
import com.pinterest.teletraan.config.AuthorizationFactory;
import com.pinterest.teletraan.config.ChatFactory;
import com.pinterest.teletraan.config.DataSourceFactory;
import com.pinterest.teletraan.config.DefaultChatFactory;
import com.pinterest.teletraan.config.DefaultEmailFactory;
import com.pinterest.teletraan.config.DefaultHostGroupFactory;
import com.pinterest.teletraan.config.DefaultSourceControlFactory;
import com.pinterest.teletraan.config.EmailFactory;
import com.pinterest.teletraan.config.EmbeddedDataSourceFactory;
import com.pinterest.teletraan.config.EventSenderFactory;
import com.pinterest.teletraan.config.ExternalAlertsConfigFactory;
import com.pinterest.teletraan.config.HostGroupFactory;
import com.pinterest.teletraan.config.JenkinsFactory;
import com.pinterest.teletraan.config.OpenAuthorizationFactory;
import com.pinterest.teletraan.config.RodimusFactory;
import com.pinterest.teletraan.config.SourceControlFactory;
import com.pinterest.teletraan.config.SystemFactory;
import com.pinterest.teletraan.config.WorkerConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import java.util.Collections;
import java.util.List;
import javax.validation.Valid;

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

    @Valid
    @JsonProperty("scm")
    private SourceControlFactory sourceControlFactory;

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
    private EventSenderFactory eventSenderFactory;

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
    @JsonProperty("externalAlerts")
    private ExternalAlertsConfigFactory externalAlertsConfigs;

    @Valid
    @JsonProperty("pingrequestvalidators")
    private List<String> pingRequestValidators;

    public DataSourceFactory getDataSourceFactory() {
        if (dataSourceFactory == null) {
            return new EmbeddedDataSourceFactory();
        }
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

    public SourceControlFactory getSourceControlFactory() {
        if (sourceControlFactory == null) {
            return new DefaultSourceControlFactory();
        }
        return sourceControlFactory;
    }

    public void setSourceControlFactory(SourceControlFactory sourceControlFactory) {
        this.sourceControlFactory = sourceControlFactory;
    }

    public AuthorizationFactory getAuthorizationFactory() {
        if (authorizationFactory == null) {
            return new OpenAuthorizationFactory();
        }
        return authorizationFactory;
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

    public EventSenderFactory getEventSenderFactory() {
        return eventSenderFactory;
    }

    public void setEventSenderFactory(EventSenderFactory eventSenderFactory) {
        this.eventSenderFactory = eventSenderFactory;
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
}
