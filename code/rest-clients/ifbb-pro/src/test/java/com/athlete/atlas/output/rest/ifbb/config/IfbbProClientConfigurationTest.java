package com.athlete.atlas.output.rest.ifbb.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.athlete.atlas.output.rest.ifbb.generated.api.IfbbEventsApi;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class IfbbProClientConfigurationTest {

  private static final String EVENTS_PATH = "/wp-json/tribe/events/v1/events";

  private WireMockServer wireMock;

  private IfbbEventsApi ifbbEventsApi;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(wireMockConfig().dynamicPort());
    wireMock.start();
    final var properties =
        new IfbbProClientProperties(
            wireMock.baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(1), "AtlasTest/1.0", 50, 200);
    final var configuration = new IfbbProClientConfiguration();
    ifbbEventsApi =
        configuration.ifbbEventsApi(configuration.ifbbApiClient(properties, RestClient.builder()));
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void sendsContractHeadersAndQueryParameters() {
    wireMock.stubFor(get(urlPathEqualTo(EVENTS_PATH)).willReturn(okJson(
        """
        {"events": [], "total": 0, "total_pages": 0}
        """)));

    ifbbEventsApi.listIfbbEvents(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30), 50, 2);

    wireMock.verify(
        getRequestedFor(urlPathEqualTo(EVENTS_PATH))
            .withHeader("User-Agent", equalTo("AtlasTest/1.0"))
            .withHeader("Accept", equalTo("application/json"))
            .withQueryParam("start_date", equalTo("2026-10-01"))
            .withQueryParam("end_date", equalTo("2027-09-30"))
            .withQueryParam("per_page", equalTo("50"))
            .withQueryParam("page", equalTo("2")));
  }

  @Test
  void readsProviderVariantsOfVenueAndImage() {
    wireMock.stubFor(get(urlPathEqualTo(EVENTS_PATH)).willReturn(okJson(
        """
        {"events": [
          {"id": 1, "title": "A", "description": "d", "url": "https://x/1", "categories": [],
           "start_date": "2026-11-01 00:00:00", "end_date": "2026-11-01 23:59:59",
           "venue": {"city": "Madrid", "country": "Spain"}, "image": false, "extra": true},
          {"id": 2, "title": "B", "description": "d", "url": "https://x/2", "categories": [],
           "start_date": "2026-11-02 00:00:00", "end_date": "2026-11-02 23:59:59",
           "venue": [], "image": {"url": "https://x/2.png"}}
        ], "total": 2, "total_pages": 1}
        """)));

    final var page = ifbbEventsApi.listIfbbEvents(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30), 50, 1);

    assertThat(page.getEvents()).hasSize(2);
    assertThat(page.getEvents().get(0).getVenue().getCity()).isEqualTo("Madrid");
    assertThat(page.getEvents().get(0).getImage()).isNull();
    assertThat(page.getEvents().get(1).getVenue()).isNull();
    assertThat(page.getEvents().get(1).getImage().getUrl()).isEqualTo("https://x/2.png");
  }

  @Test
  void serverErrorsSurfaceAsHttpServerErrorException() {
    wireMock.stubFor(get(urlPathEqualTo(EVENTS_PATH)).willReturn(aResponse().withStatus(503)));

    assertThatThrownBy(
            () -> ifbbEventsApi.listIfbbEvents(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30), 50, 1))
        .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);
  }
}
