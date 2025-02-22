/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

import com.pinterest.teletraan.health.GenericHealthCheck;
import com.pinterest.teletraan.resource.Pings;
import com.pinterest.teletraan.universal.security.PrincipalNameInjector;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfoFeature;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class TeletraanAgentService extends Application<TeletraanServiceConfiguration> {
    @Override
    public String getName() {
        return "teletraan-agent-service";
    }

    @Override
    public void initialize(Bootstrap<TeletraanServiceConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(TeletraanServiceConfiguration configuration, Environment environment)
            throws Exception {
        TeletraanServiceContext context = ConfigHelper.setupContext(configuration, environment);
        environment.jersey().register(context);
        environment
                .jersey()
                .register(
                        new AuthDynamicFeature(
                                configuration.getAuthenticationFactory().create(context)));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(ResourceAuthZInfoFeature.class);
        environment.jersey().register(Pings.class);
        environment.jersey().register(PrincipalNameInjector.class);

        environment.healthChecks().register("generic", new GenericHealthCheck(context));
    }

    public static void main(String[] args) throws Exception {
        new TeletraanAgentService().run(args);
    }
}
