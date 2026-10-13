package com.athlete.atlas.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Activa la abstraccion de cache de Spring. El proveedor es Caffeine (cache en memoria del propio
 * servicio) y se configura en {@code spring.cache.*} de application.yml.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {}
