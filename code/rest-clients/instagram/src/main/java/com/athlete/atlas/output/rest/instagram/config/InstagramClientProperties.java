package com.athlete.atlas.output.rest.instagram.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the Facebook Graph API client used for Instagram Business Discovery.
 *
 * @param apiVersion Graph API version, e.g. v26.0.
 * @param authorizedAccountId Instagram business account authorized to run the query.
 * @param accessToken server-side access token. Never logged: {@link #toString()} masks it. It
 *     expires and must be renewed outside this integration.
 * @param mediaLimit media requested in the single Business Discovery request.
 * @param selectionSize number of posts to select.
 */
@ConfigurationProperties(prefix = "rest-clients.instagram")
public record InstagramClientProperties(
    String baseUrl,
    String apiVersion,
    String authorizedAccountId,
    String accessToken,
    Duration connectTimeout,
    Duration readTimeout,
    @DefaultValue("50") int mediaLimit,
    @DefaultValue("3") int selectionSize) {

  @Override
  public String toString() {
    return "InstagramClientProperties[baseUrl=%s, apiVersion=%s, authorizedAccountId=%s, accessToken=%s, connectTimeout=%s, readTimeout=%s, mediaLimit=%d, selectionSize=%d]"
        .formatted(
            baseUrl,
            apiVersion,
            authorizedAccountId,
            accessToken == null || accessToken.isBlank() ? "<unset>" : "******",
            connectTimeout,
            readTimeout,
            mediaLimit,
            selectionSize);
  }
}
