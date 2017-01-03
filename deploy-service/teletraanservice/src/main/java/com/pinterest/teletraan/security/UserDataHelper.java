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
package com.pinterest.teletraan.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.common.HTTPClient;
import com.pinterest.teletraan.TeletraanServiceContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UserDataHelper {
    private String userDataUrl;
    private String groupDataUrl;
    private String[] userNameKeys;
    private Boolean extractUserNameFromEmail;
    private LoadingCache<String, UserSecurityContext> tokenCache;
    private TeletraanServiceContext context;
    private static final Logger LOG = LoggerFactory.getLogger(UserDataHelper.class);

    public UserDataHelper(String userDataUrl, String groupDataUrl, String userNameKey, Boolean extractUserNameFromEmail, String tokenCacheSpec, TeletraanServiceContext context) {
        this.userDataUrl = userDataUrl;
        this.groupDataUrl = groupDataUrl;
        this.context = context;
        this.extractUserNameFromEmail = extractUserNameFromEmail;
        if (!StringUtils.isEmpty(tokenCacheSpec)) {
            tokenCache = CacheBuilder.from(tokenCacheSpec)
                .build(new CacheLoader<String, UserSecurityContext>() {
                    @Override
                    public UserSecurityContext load(String token) throws Exception {
                        return loadOauthUserData(token);
                    }
                });
        }

        // prepare username keys
        if (StringUtils.isEmpty(userNameKey)) {
            userNameKeys = null;
        } else {
            userNameKeys = userNameKey.split("\\s+");
        }
    }

    public List<String> getUserGroups(String token) throws Exception {
        if (StringUtils.isEmpty(groupDataUrl)) {
            return Collections.emptyList();
        }

        // Get user groups through auth server with user oauth token
        HTTPClient client = new HTTPClient();
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", token);
        String jsonResponse = client.get(groupDataUrl, null, params, null, 3);

        // Parse response
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(jsonResponse);

        if (element.getAsJsonObject().has("groups")) {
            JsonArray jsonArray = element.getAsJsonObject().getAsJsonArray("groups");
            String[] groups = gson.fromJson(jsonArray, String[].class);
            LOG.info("Retrieved groups " + Arrays.asList(groups).toString() + " from token.");
            return Arrays.asList(groups);
        }

        return null;
    }

    public String getUsername(String token) throws Exception {
        HTTPClient httpClient = new HTTPClient();
        Map<String, String> params = ImmutableMap.of("access_token", token);
        String jsonPayload = httpClient.get(userDataUrl, null, params, null, 3);
        JsonElement e = new JsonParser().parse(jsonPayload);
        String userName;
        if (userNameKeys != null && userNameKeys.length > 0) {
            JsonObject jsonObject = e.getAsJsonObject();
            int i = 0;
            for (; i < userNameKeys.length - 1; i++) {
                jsonObject = jsonObject.getAsJsonObject(userNameKeys[i]);
            }
            userName = jsonObject.get(userNameKeys[i]).getAsString();
        } else {
            userName = e.getAsString();
        }
        if (extractUserNameFromEmail != null && extractUserNameFromEmail) {
            userName = userName.split("@")[0];
        }
        LOG.info("Retrieved username " + userName + " from token.");
        return userName;
    }

    public UserSecurityContext loadOauthUserData(String token) throws Exception {
        TokenRolesBean tokenRolesBean = context.getTokenRolesDAO().getByToken(token);

        // Script token
        if (tokenRolesBean != null) {
            return new UserSecurityContext(tokenRolesBean.getScript_name(), tokenRolesBean, null);
        }

        // User token
        String username = getUsername(token);
        List<String> groups = getUserGroups(token);
        return new UserSecurityContext(username, null, groups);
    }

    public UserSecurityContext getUserSecurityContext(String token) throws Exception {
        if (tokenCache == null) {
            return loadOauthUserData(token);
        } else {
            return tokenCache.get(token);
        }
    }
}
