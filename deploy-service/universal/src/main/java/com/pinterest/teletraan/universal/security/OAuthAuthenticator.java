package com.pinterest.teletraan.universal.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.dropwizard.auth.Authenticator;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Deprecated
public class OAuthAuthenticator implements Authenticator<String, UserPrincipal> {
  private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticator.class);

  private final String groupDataUrl;
  private final HttpClient userDataClient;
  private final HttpClient groupDataClient;

  public OAuthAuthenticator(String userDataUrl, String groupDataUrl) {
    HttpClient baseClient = HttpClient.create()
                                     .responseTimeout(Duration.ofSeconds(3))
                                     ;
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

  private List<String> getUserGroups(String token) throws Exception {
    if (StringUtils.isEmpty(groupDataUrl)) {
      return Collections.emptyList();
    }

    // Get user groups through auth server with user oauth token
    String uri = String.format("?access_token=%s", token);
    String jsonResponse = groupDataClient
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
      LOG.debug("Retrieved groups " + Arrays.asList(groups).toString() + " from token.");
      return Arrays.asList(groups);
    }

    return Collections.emptyList();
  }

  private String getUsername(String token) throws Exception {
    String uri = String.format("?access_token=%s", token);
    String jsonResponse = userDataClient
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
    LOG.debug("Retrieved username " + userName + " from token.");
    return userName;
  }
}
