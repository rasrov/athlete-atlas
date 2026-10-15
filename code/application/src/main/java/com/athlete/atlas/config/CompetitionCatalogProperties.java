package com.athlete.atlas.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application configuration of the competition catalog.
 *
 * @param freshnessThreshold age after which the catalog is reported as stale even if its last
 *     refresh succeeded.
 */
@ConfigurationProperties(prefix = "competitions")
public record CompetitionCatalogProperties(@DefaultValue("12h") Duration freshnessThreshold) {}
