package com.athlete.atlas.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application configuration of the Instagram post selection. The username of each athlete is
 * stored in the athlete table, which only accepts valid usernames; the Instagram adapter validates
 * it again before building the Graph API query.
 *
 * @param freshnessThreshold age after which a selection is reported as stale even if its last
 *     refresh succeeded.
 */
@ConfigurationProperties(prefix = "instagram")
public record InstagramSelectionProperties(@DefaultValue("12h") Duration freshnessThreshold) {}
