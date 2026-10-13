package com.athlete.atlas.output.rest.ifbb.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rest-clients.ifbb-pro")
public record IfbbProClientProperties(
    String baseUrl,
    Duration connectTimeout,
    Duration readTimeout,
    String userAgent,
    @DefaultValue("50") int pageSize,
    @DefaultValue("200") int maxPages) {}
