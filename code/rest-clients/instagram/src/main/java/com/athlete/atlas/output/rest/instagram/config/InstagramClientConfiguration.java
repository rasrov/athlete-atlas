package com.athlete.atlas.output.rest.instagram.config;

import com.athlete.atlas.output.rest.instagram.generated.ApiClient;
import com.athlete.atlas.output.rest.instagram.generated.api.InstagramGraphApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(InstagramClientProperties.class)
public class InstagramClientConfiguration {

  /** The access token is sent as an Authorization Bearer header, never in URLs. */
  @Bean
  public ApiClient instagramApiClient(
      final InstagramClientProperties instagramClientProperties,
      final RestClient.Builder restClientBuilder) {
    final var requestFactory =
        ClientHttpRequestFactoryBuilder.detect()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(instagramClientProperties.connectTimeout())
                    .withReadTimeout(instagramClientProperties.readTimeout()));
    final var restClient = restClientBuilder.requestFactory(requestFactory).build();

    final var apiClient = new ApiClient(restClient);
    apiClient.setBasePath(instagramClientProperties.baseUrl());
    final var accessToken = instagramClientProperties.accessToken();
    if (accessToken == null || accessToken.isBlank()) {
      log.warn(
          "Instagram access token is not configured (INSTAGRAM_ACCESS_TOKEN): Instagram refreshes"
              + " will fail");
    } else {
      apiClient.setBearerToken(accessToken);
    }
    final var accountId = instagramClientProperties.authorizedAccountId();
    if (accountId == null || accountId.isBlank()) {
      log.warn(
          "Instagram authorized account is not configured (INSTAGRAM_AUTHORIZED_ACCOUNT_ID):"
              + " Instagram refreshes will fail");
    }
    return apiClient;
  }

  @Bean
  public InstagramGraphApi instagramGraphApi(final ApiClient instagramApiClient) {
    return new InstagramGraphApi(instagramApiClient);
  }
}
