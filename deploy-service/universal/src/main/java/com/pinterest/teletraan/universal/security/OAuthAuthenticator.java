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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import io.dropwizard.auth.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

/**
 * @deprecated Do not use this class for new development. It is kept here for backward compatibility
 *     OAuthAuthenticator is an authenticator that authenticates a user using OAuth token.
 */
@Deprecated
@Slf4j
public class OAuthAuthenticator implements Authenticator<String, UserPrincipal> {
    private static final String USER = "user";
    private static final String GROUPS = "groups";
    private static final String USERNAME_FIELD = "username";
    private static final String ACCESS_TOKEN_QUERY = "/?access_token=%s";

    private final String groupDataUrl;
    private final HttpClient userDataClient;
    private final HttpClient groupDataClient;

    public OAuthAuthenticator(String userDataUrl, String groupDataUrl)
            throws MalformedURLException {
        if (StringUtils.isBlank(userDataUrl)) {
            throw new IllegalArgumentException("User data url cannot be empty");
        }
        userDataUrl = sanitizeUrl(userDataUrl);
        groupDataUrl = sanitizeUrl(groupDataUrl);
        HttpClient baseClient = HttpClient.create().responseTimeout(Duration.ofSeconds(3));
        userDataClient = baseClient.baseUrl(userDataUrl);
        groupDataClient = baseClient.baseUrl(groupDataUrl);
        this.groupDataUrl = groupDataUrl;
    }

    @Override
    public Optional<UserPrincipal> authenticate(String token) {
        log.debug("Authenticating...");
        try {
            String username = getUsername(token);
            List<String> groups = getUserGroups(token);
            return Optional.of(new UserPrincipal(username, groups));
        } catch (Exception exception) {
            log.debug("authN failed", exception);
            return Optional.empty();
        }
    }

    private List<String> getUserGroups(String token) {
        if (groupDataUrl.isEmpty()) {
            return Collections.emptyList();
        }

        // Get user groups through auth server with user oauth token
        String jsonResponse =
                groupDataClient
                        .get()
                        .uri(getUriString(token))
                        .responseContent()
                        .aggregate()
                        .asString()
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).jitter(0.75))
                        .block();

        // Parse response
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            if (jsonNode.has(GROUPS)) {
                JsonNode groupsNode = jsonNode.get(GROUPS);
                String[] groups = objectMapper.convertValue(groupsNode, String[].class);
                log.debug("Retrieved groups {} from token.", Arrays.asList(groups));
                return Arrays.asList(groups);
            }
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse {}", jsonResponse, e);
        }
        return Collections.emptyList();
    }

    private String getUsername(String token) throws JsonProcessingException {
        String jsonResponse =
                userDataClient
                        .get()
                        .uri(getUriString(token))
                        .responseContent()
                        .aggregate()
                        .asString()
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).jitter(0.75))
                        .block();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        JsonNode userNode = jsonNode.get(USER);
        String userName = userNode.get(USERNAME_FIELD).asText();
        log.debug("Retrieved username {} from identity provider.", userName);
        return userName;
    }

    private static String getUriString(String token) {
        return String.format(ACCESS_TOKEN_QUERY, token);
    }

    private static String sanitizeUrl(String url) throws MalformedURLException {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        url = url.trim();
        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        new URL(url);
        return url;
    }
}
