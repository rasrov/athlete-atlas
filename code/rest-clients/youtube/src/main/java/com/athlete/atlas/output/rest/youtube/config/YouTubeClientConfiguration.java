package com.athlete.atlas.output.rest.youtube.config;

import com.athlete.atlas.output.rest.youtube.generated.ApiClient;
import com.athlete.atlas.output.rest.youtube.generated.api.YouTubeDataApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(YouTubeClientProperties.class)
public class YouTubeClientConfiguration {

  /**
   * The API key is sent in the X-goog-api-key header (security scheme of the contract), never as
   * the key query parameter, so it cannot leak through URLs in exception messages or logs.
   */
  @Bean
  public ApiClient youTubeApiClient(
      final YouTubeClientProperties youTubeClientProperties,
      final RestClient.Builder restClientBuilder) {
    final var requestFactory =
        ClientHttpRequestFactoryBuilder.detect()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(youTubeClientProperties.connectTimeout())
                    .withReadTimeout(youTubeClientProperties.readTimeout()));
    final var restClient = restClientBuilder.requestFactory(requestFactory).build();

    final var apiClient = new ApiClient(restClient);
    apiClient.setBasePath(youTubeClientProperties.baseUrl());
    final var apiKey = youTubeClientProperties.apiKey();
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("YouTube API key is not configured (YOUTUBE_API_KEY): YouTube refreshes will fail");
    } else {
      apiClient.setApiKey(apiKey);
    }
    return apiClient;
  }

  @Bean
  public YouTubeDataApi youTubeDataApi(final ApiClient youTubeApiClient) {
    return new YouTubeDataApi(youTubeApiClient);
  }
}
