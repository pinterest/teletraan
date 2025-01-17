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

import com.pinterest.teletraan.health.WorkerHealthCheck;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class TeletraanWorker extends Application<TeletraanServiceConfiguration> {
    @Override
    public String getName() {
        return "teletraan-worker";
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
        ConfigHelper.scheduleWorkers(configuration, context);
        environment.healthChecks().register("generic", new WorkerHealthCheck(context));
    }

    public static void main(String[] args) throws Exception {
        new TeletraanWorker().run(args);
    }
}
