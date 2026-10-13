package com.athlete.atlas.output.rest.ifbb.config;

import com.athlete.atlas.output.rest.ifbb.generated.ApiClient;
import com.athlete.atlas.output.rest.ifbb.generated.api.IfbbEventsApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(IfbbProClientProperties.class)
public class IfbbProClientConfiguration {

  @Bean
  public ApiClient ifbbApiClient(
      final IfbbProClientProperties ifbbProClientProperties,
      final RestClient.Builder restClientBuilder) {
    final var requestFactory =
        ClientHttpRequestFactoryBuilder.detect()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(ifbbProClientProperties.connectTimeout())
                    .withReadTimeout(ifbbProClientProperties.readTimeout()));
    final var restClient =
        restClientBuilder
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.USER_AGENT, ifbbProClientProperties.userAgent())
            .messageConverters(IfbbJsonConfiguration::configure)
            .build();

    final var apiClient = new ApiClient(restClient);
    apiClient.setBasePath(ifbbProClientProperties.baseUrl());
    return apiClient;
  }

  @Bean
  public IfbbEventsApi ifbbEventsApi(final ApiClient ifbbApiClient) {
    return new IfbbEventsApi(ifbbApiClient);
  }
}
