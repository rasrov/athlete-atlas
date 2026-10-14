package com.athlete.atlas.config;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application configuration of the Instagram post selection.
 *
 * @param usernames Instagram username of each athlete, keyed by athlete id. They are validated on
 *     startup because they end up in the Graph API query. TODO: move this association to storage
 *     once the backend has a database.
 * @param freshnessThreshold age after which a selection is reported as stale even if its last
 *     refresh succeeded.
 */
@ConfigurationProperties(prefix = "instagram")
public record InstagramSelectionProperties(
    @DefaultValue Map<String, String> usernames,
    @DefaultValue("12h") Duration freshnessThreshold) {

  /** Instagram usernames: letters, digits, dots and underscores, up to 30 characters. */
  private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._]{1,30}$");

  public InstagramSelectionProperties {
    usernames.forEach(
        (athleteId, username) -> {
          if (username == null || !USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException(
                "Invalid Instagram username configured for athlete " + athleteId);
          }
        });
  }
}
