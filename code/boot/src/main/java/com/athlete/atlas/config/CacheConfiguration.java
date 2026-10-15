package com.athlete.atlas.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the Spring cache abstraction for the data owned by the backend (e.g. confirmed
 * competitions). The provider is Caffeine, configured in spring.cache of application.yml.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {}
