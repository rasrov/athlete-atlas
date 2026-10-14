package com.athlete.atlas.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Configuration of the synchronized selections served by the application (YouTube, Instagram). */
@Configuration
@EnableConfigurationProperties({
  YouTubeSelectionProperties.class,
  InstagramSelectionProperties.class
})
public class SelectionConfiguration {}
