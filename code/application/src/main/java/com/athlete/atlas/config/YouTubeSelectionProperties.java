package com.athlete.atlas.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application configuration of the YouTube video selection. The channel of each athlete is stored
 * in the athlete table.
 *
 * @param freshnessThreshold age after which a selection is reported as stale even if its last
 *     refresh succeeded.
 */
@ConfigurationProperties(prefix = "youtube")
public record YouTubeSelectionProperties(@DefaultValue("12h") Duration freshnessThreshold) {}
