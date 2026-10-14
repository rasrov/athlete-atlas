package com.athlete.atlas.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application configuration of the YouTube video selection.
 *
 * @param channels YouTube channel id of each athlete, keyed by athlete id. TODO: move this
 *     association to storage once the backend has a database, so that athletes and their channels
 *     can be managed without a redeploy.
 * @param freshnessThreshold age after which a selection is reported as stale even if its last
 *     refresh succeeded.
 */
@ConfigurationProperties(prefix = "youtube")
public record YouTubeSelectionProperties(
    @DefaultValue Map<String, String> channels,
    @DefaultValue("12h") Duration freshnessThreshold) {}
