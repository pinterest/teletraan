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

import com.pinterest.teletraan.exception.GenericExceptionMapper;
import com.pinterest.teletraan.health.GenericHealthCheck;
import com.pinterest.teletraan.health.HealthCheckController;
import com.pinterest.teletraan.resource.*;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

public class TeletraanService extends Application<TeletraanServiceConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(TeletraanService.class);

    @Override
    public String getName() {
        return "teletraan-service";
    }

    @Override
    public void initialize(Bootstrap<TeletraanServiceConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)
            )
        );
    }

    @Override
    public void run(TeletraanServiceConfiguration configuration, Environment environment) throws Exception {
        TeletraanServiceContext context = ConfigHelper.setupContext(configuration);

        environment.jersey().register(configuration.getAuthenticationFactory().create(context));

        Builds builds = new Builds(context);
        environment.jersey().register(builds);

        Commits commits = new Commits(context);
        environment.jersey().register(commits);

        Deploys deploys = new Deploys(context);
        environment.jersey().register(deploys);

        Agents agents = new Agents(context);
        environment.jersey().register(agents);

        EnvAgentConfigs envAdvancedConfigs = new EnvAgentConfigs(context);
        environment.jersey().register(envAdvancedConfigs);

        EnvAgents envAgents = new EnvAgents(context);
        environment.jersey().register(envAgents);

        EnvAlarms envAlarms = new EnvAlarms(context);
        environment.jersey().register(envAlarms);

        EnvDeploys envDeploys = new EnvDeploys(context);
        environment.jersey().register(envDeploys);

        EnvCapacities envCapacitys = new EnvCapacities(context);
        environment.jersey().register(envCapacitys);

        Environs envs = new Environs(context);
        environment.jersey().register(envs);

        EnvStages envStages = new EnvStages(context);
        environment.jersey().register(envStages);

        EnvMetrics envMetrics = new EnvMetrics(context);
        environment.jersey().register(envMetrics);

        EnvHistory envHistory = new EnvHistory(context);
        environment.jersey().register(envHistory);

        EnvPromotes envPromotes = new EnvPromotes(context);
        environment.jersey().register(envPromotes);

        EnvScriptConfigs envScriptConfigs = new EnvScriptConfigs(context);
        environment.jersey().register(envScriptConfigs);

        EnvTokenRoles envTokenRoles = new EnvTokenRoles(context);
        environment.jersey().register(envTokenRoles);

        EnvUserRoles envUserRoles = new EnvUserRoles(context);
        environment.jersey().register(envUserRoles);

        EnvWebHooks envWebHooks = new EnvWebHooks(context);
        environment.jersey().register(envWebHooks);

        EnvHosts envHosts = new EnvHosts(context);
        environment.jersey().register(envHosts);

        EnvHostTags envHostTags = new EnvHostTags(context);
        environment.jersey().register(envHostTags);

        DeployConstraints deployConstraints = new DeployConstraints(context);
        environment.jersey().register(deployConstraints);

        Hotfixs hotfixes = new Hotfixs(context);
        environment.jersey().register(hotfixes);

        Ratings ratings = new Ratings(context);
        environment.jersey().register(ratings);

        SystemGroupRoles systemGroups = new SystemGroupRoles(context);
        environment.jersey().register(systemGroups);

        EnvGroupRoles envGroups = new EnvGroupRoles(context);
        environment.jersey().register(envGroups);

        Hosts hosts = new Hosts(context);
        environment.jersey().register(hosts);

        Systems systems = new Systems(context);
        environment.jersey().register(systems);

        // Support pings as well
        Pings pings = new Pings(context);
        environment.jersey().register(pings);

        DeployCandidates buildCandidates = new DeployCandidates(context);
        environment.jersey().register(buildCandidates);

        Schedules schedules = new Schedules(context);
        environment.jersey().register(schedules);

        environment.jersey().register(new Tags(context));

        Groups groups = new Groups(context);
        environment.jersey().register(groups);

        EnvAlerts envAlerts = new EnvAlerts(context);
        environment.jersey().register(envAlerts);

        // Schedule workers if configured
        ConfigHelper.scheduleWorkers(configuration, context);

        environment.healthChecks().register("generic", new GenericHealthCheck(context));
        environment.jersey().register(new HealthCheckController(environment.healthChecks()));

        // Exception handler
        environment.jersey().register(new GenericExceptionMapper(configuration.getSystemFactory().getClientError()));

        // Swagger API docs generation related
        environment.jersey().register(new ApiListingResource());
        environment.jersey().register(new SwaggerSerializers());
        BeanConfig config = new BeanConfig();
        config.setTitle("Teletraan API Docs");
        config.setVersion("1.0.0");
        config.setResourcePackage("com.pinterest.teletraan.resource");
        config.setScan(true);

        // Enable CORS headers
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
        filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true");
        filter.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
        filter.setInitParameter("allowCredentials", "true");

    }


    public static void main(String[] args) throws Exception {
        new TeletraanService().run(args);
    }
}
