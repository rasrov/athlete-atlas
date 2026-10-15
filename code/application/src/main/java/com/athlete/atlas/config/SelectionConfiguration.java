package com.athlete.atlas.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of the synchronized data served by the application (competitions, YouTube,
 * Instagram).
 */
@Configuration
@EnableConfigurationProperties({
  CompetitionCatalogProperties.class,
  YouTubeSelectionProperties.class,
  InstagramSelectionProperties.class
})
public class SelectionConfiguration {}
