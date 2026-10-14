package com.athlete.atlas.output.rest.instagram.client;

import com.athlete.atlas.output.rest.instagram.config.InstagramClientProperties;
import com.athlete.atlas.output.rest.instagram.generated.api.InstagramGraphApi;
import com.athlete.atlas.output.rest.instagram.generated.model.BusinessDiscoveryResponse;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Transport layer for the Facebook Graph API. Retries are applied here and only here, so that they
 * are not multiplied; which errors are retried is configured under {@code
 * resilience4j.retry.instances.instagramGraphApi}. Spring REST client exceptions are propagated
 * untouched so that the caller can translate them.
 */
@Component
@RequiredArgsConstructor
public class InstagramGraphClient {

  public static final String BOUNDED_CONTEXT = "instagram_graph_api";

  private static final String RESILIENCE_INSTANCE = "instagramGraphApi";

  private final InstagramGraphApi instagramGraphApi;

  private final InstagramClientProperties instagramClientProperties;

  /**
   * Runs a Business Discovery query through the authorized account.
   *
   * @param fields already built fields parameter; the client encodes it.
   */
  @Retry(name = RESILIENCE_INSTANCE)
  public BusinessDiscoveryResponse fetchBusinessDiscovery(@Nonnull final String fields) {
    return instagramGraphApi.getBusinessDiscovery(
        instagramClientProperties.apiVersion(),
        instagramClientProperties.authorizedAccountId(),
        fields);
  }
}
