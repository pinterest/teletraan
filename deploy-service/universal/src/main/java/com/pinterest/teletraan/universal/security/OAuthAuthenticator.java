/**
 * Copyright (c) 2024 Pinterest, Inc.
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
package com.pinterest.teletraan.universal.security;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import io.dropwizard.auth.Authenticator;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

/**
 * @deprecated Do not use this class for new development. It is kept here for backward compatibility
 *     OAuthAuthenticator is an authenticator that authenticates a user using OAuth token.
 */
@Deprecated
public class OAuthAuthenticator implements Authenticator<String, UserPrincipal> {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticator.class);

    private final String groupDataUrl;
    private final HttpClient userDataClient;
    private final HttpClient groupDataClient;

    public OAuthAuthenticator(String userDataUrl, String groupDataUrl) {
        HttpClient baseClient = HttpClient.create().responseTimeout(Duration.ofSeconds(3));
        userDataClient = baseClient.baseUrl(userDataUrl);
        groupDataClient = baseClient.baseUrl(groupDataUrl);
        this.groupDataUrl = groupDataUrl;
    }

    @Override
    public Optional<UserPrincipal> authenticate(String token) {
        LOG.debug("Authenticating...");
        try {
            String username = getUsername(token);
            List<String> groups = getUserGroups(token);
            return Optional.of(new UserPrincipal(username, groups));
        } catch (Exception exception) {
            LOG.debug("authN failed", exception);
            return Optional.empty();
        }
    }

    private List<String> getUserGroups(String token) {
        if (StringUtils.isEmpty(groupDataUrl)) {
            return Collections.emptyList();
        }

        // Get user groups through auth server with user oauth token
        String uri = String.format("?access_token=%s", token);
        String jsonResponse =
                groupDataClient
                        .get()
                        .uri(uri)
                        .responseContent()
                        .aggregate()
                        .asString()
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).jitter(0.75))
                        .block();

        // Parse response
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(jsonResponse);

        if (element.getAsJsonObject().has("groups")) {
            JsonArray jsonArray = element.getAsJsonObject().getAsJsonArray("groups");
            String[] groups = gson.fromJson(jsonArray, String[].class);
            LOG.debug("Retrieved groups {} from token.", Arrays.asList(groups));
            return Arrays.asList(groups);
        }

        return Collections.emptyList();
    }

    private String getUsername(String token) {
        String uri = String.format("?access_token=%s", token);
        String jsonResponse =
                userDataClient
                        .get()
                        .uri(uri)
                        .responseContent()
                        .aggregate()
                        .asString()
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).jitter(0.75))
                        .block();
        JsonObject jsonObject = new JsonParser().parse(jsonResponse).getAsJsonObject();
        JsonObject userObject = jsonObject.getAsJsonObject("user");
        String userName = userObject.get("username").getAsString();
        LOG.debug("Retrieved username {} from token.", userName);
        return userName;
    }
}
