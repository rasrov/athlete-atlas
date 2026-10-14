package com.athlete.atlas.output.rest.youtube.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the YouTube Data API client.
 *
 * @param apiKey server-side Google API key. Never logged: {@link #toString()} masks it.
 * @param pageSize playlist items requested per page (maxResults, at most 50).
 * @param maxPages maximum playlist pages read per selection.
 * @param selectionSize number of videos to select.
 */
@ConfigurationProperties(prefix = "rest-clients.youtube")
public record YouTubeClientProperties(
    String baseUrl,
    String apiKey,
    Duration connectTimeout,
    Duration readTimeout,
    @DefaultValue("50") int pageSize,
    @DefaultValue("4") int maxPages,
    @DefaultValue("6") int selectionSize) {

  @Override
  public String toString() {
    return "YouTubeClientProperties[baseUrl=%s, apiKey=%s, connectTimeout=%s, readTimeout=%s, pageSize=%d, maxPages=%d, selectionSize=%d]"
        .formatted(
            baseUrl,
            apiKey == null || apiKey.isBlank() ? "<unset>" : "******",
            connectTimeout,
            readTimeout,
            pageSize,
            maxPages,
            selectionSize);
  }
}
