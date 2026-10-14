package com.athlete.atlas.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(YouTubeSelectionProperties.class)
public class YouTubeSelectionConfiguration {}
