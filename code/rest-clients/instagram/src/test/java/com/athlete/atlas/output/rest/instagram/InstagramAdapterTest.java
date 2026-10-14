package com.athlete.atlas.output.rest.instagram;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.instagram.InstagramMediaType;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.output.rest.instagram.client.InstagramGraphClient;
import com.athlete.atlas.output.rest.instagram.config.InstagramClientConfiguration;
import com.athlete.atlas.output.rest.instagram.mapper.InstagramPostMapperImpl;
import com.athlete.atlas.service.InvokerServiceImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Exercises the selection against simulated Graph API responses, with the real generated client,
 * JSON mapping and retries (waits shortened to 1ms).
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
    classes = InstagramAdapterTest.TestApplication.class,
    properties = {
      "rest-clients.instagram.api-version=v26.0",
      "rest-clients.instagram.authorized-account-id=" + InstagramAdapterTest.ACCOUNT_ID,
      "rest-clients.instagram.access-token=" + InstagramAdapterTest.TOKEN,
      "rest-clients.instagram.connect-timeout=1s",
      "rest-clients.instagram.read-timeout=500ms",
      "rest-clients.instagram.media-limit=50",
      "rest-clients.instagram.selection-size=3",
      "resilience4j.retry.instances.instagramGraphApi.max-attempts=3",
      "resilience4j.retry.instances.instagramGraphApi.wait-duration=1ms",
      "resilience4j.retry.instances.instagramGraphApi.retry-exceptions="
          + "org.springframework.web.client.HttpClientErrorException$TooManyRequests,"
          + "org.springframework.web.client.HttpServerErrorException$InternalServerError,"
          + "org.springframework.web.client.HttpServerErrorException$BadGateway,"
          + "org.springframework.web.client.HttpServerErrorException$ServiceUnavailable,"
          + "org.springframework.web.client.HttpServerErrorException$GatewayTimeout,"
          + "org.springframework.web.client.ResourceAccessException"
    })
class InstagramAdapterTest {

  static final String TOKEN = "TEST-SECRET-ACCESS-TOKEN-5678";

  static final String ACCOUNT_ID = "17841401870042081";

  private static final String USERNAME = "kim_angel";

  private static final String PATH = "/v26.0/" + ACCOUNT_ID;

  private static final String FIELDS =
      "business_discovery.username(kim_angel){username,media.limit(50)"
          + "{id,permalink,media_type,media_url,thumbnail_url,timestamp}}";

  private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

  private static final String SIGNED_IMAGE = "https://scontent.cdninstagram.com/img.jpg?oh=SIGNATURE";

  private static final WireMockServer WIRE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    WIRE_MOCK.start();
  }

  @SpringBootConfiguration
  @ImportAutoConfiguration({
    AopAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    RestClientAutoConfiguration.class,
    io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration.class
  })
  @Import({
    InstagramClientConfiguration.class,
    InstagramGraphClient.class,
    InstagramAdapter.class,
    InstagramPostMapperImpl.class,
    InvokerServiceImpl.class
  })
  static class TestApplication {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }

  @DynamicPropertySource
  static void graphBaseUrl(final DynamicPropertyRegistry registry) {
    registry.add("rest-clients.instagram.base-url", WIRE_MOCK::baseUrl);
  }

  @Autowired private InstagramAdapter instagramAdapter;

  @BeforeEach
  void resetStubs() {
    WIRE_MOCK.resetAll();
  }

  @AfterAll
  static void stopWireMock() {
    WIRE_MOCK.stop();
  }

  @Test
  void requestsTheProfileWithPathFieldsAndBearerToken() {
    givenMedia(post("1", "IMAGE", "2026-10-13T10:00:00+0000"));

    instagramAdapter.fetchLatestPosts(USERNAME);

    WIRE_MOCK.verify(
        1,
        getRequestedFor(urlPathEqualTo(PATH))
            .withQueryParam("fields", equalTo(FIELDS))
            .withHeader("Authorization", equalTo("Bearer " + TOKEN)));
    assertThat(WIRE_MOCK.getAllServeEvents().getFirst().getRequest().getUrl())
        .doesNotContain(TOKEN);
  }

  @Test
  void selectsTheThreeLatestPostsWhateverTheResponseOrder() {
    givenMedia(
        post("1", "IMAGE", "2026-10-10T10:00:00+0000"),
        post("2", "IMAGE", "2026-10-13T10:00:00+0000"),
        post("3", "IMAGE", "2026-10-11T10:00:00+0000"),
        post("4", "IMAGE", "2026-10-12T10:00:00+0000"),
        post("5", "IMAGE", "2026-10-09T10:00:00+0000"));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .extracting(InstagramPost::id)
        .containsExactly("2", "4", "3");
  }

  @Test
  void breaksTiesByNumericIdAscending() {
    givenMedia(
        post("10", "IMAGE", "2026-10-13T10:00:00+0000"),
        post("9", "IMAGE", "2026-10-13T10:00:00+0000"),
        post("11", "IMAGE", "2026-10-13T10:00:00+0000"));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .extracting(InstagramPost::id)
        .containsExactly("9", "10", "11");
  }

  @Test
  void keepsASinglePostPerId() {
    givenMedia(
        post("1", "IMAGE", "2026-10-13T10:00:00+0000"),
        post("1", "VIDEO", "2026-10-13T11:00:00+0000"),
        post("2", "IMAGE", "2026-10-12T10:00:00+0000"));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .extracting(InstagramPost::id, InstagramPost::mediaType)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("1", InstagramMediaType.IMAGE),
            org.assertj.core.groups.Tuple.tuple("2", InstagramMediaType.IMAGE));
  }

  @Test
  void supportsImagesVideosAndCarouselsAndSkipsOtherTypes() {
    givenMedia(
        post("1", "IMAGE", "2026-10-13T10:00:00+0000"),
        post("2", "VIDEO", "2026-10-12T10:00:00+0000"),
        post("3", "STORY", "2026-10-13T11:00:00+0000"),
        post("4", "CAROUSEL_ALBUM", "2026-10-11T10:00:00+0000"));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .extracting(InstagramPost::mediaType)
        .containsExactly(
            InstagramMediaType.IMAGE, InstagramMediaType.VIDEO, InstagramMediaType.CAROUSEL_ALBUM);
  }

  @Test
  void mapsDatesUrlsAndCovers() {
    givenMedia(
        """
        {"id": "2", "media_type": "VIDEO", "permalink": "https://www.instagram.com/reel/XyZ/",
         "media_url": "https://scontent.cdninstagram.com/video.mp4",
         "thumbnail_url": "https://scontent.cdninstagram.com/thumb.jpg",
         "timestamp": "2026-10-13T12:00:00+0200"}
        """);

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .containsExactly(
            new InstagramPost(
                "2",
                InstagramMediaType.VIDEO,
                Instant.parse("2026-10-13T10:00:00Z"),
                URI.create("https://www.instagram.com/reel/XyZ/"),
                URI.create("https://scontent.cdninstagram.com/thumb.jpg")));
  }

  @Test
  void skipsFuturePosts() {
    givenMedia(
        post("1", "IMAGE", "2026-10-14T12:00:01+0000"),
        post("2", "IMAGE", "2026-10-14T12:00:00+0000"));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .extracting(InstagramPost::id)
        .containsExactly("2");
  }

  @Test
  void discardsPostsWithInvalidDataAndKeepsTheRest(final CapturedOutput output) {
    givenMedia(
        post("abc", "IMAGE", "2026-10-13T10:00:00+0000"),
        """
        {"id": "2", "media_type": "IMAGE", "permalink": "https://www.instagram.com/p/AbC/?utm=x",
         "timestamp": "2026-10-13T10:00:00+0000"}
        """,
        post("3", "IMAGE", "2026-10-13T10:00:00"),
        post("4", "IMAGE", "2026-10-12T10:00:00+0000"));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .extracting(InstagramPost::id)
        .containsExactly("4");
    assertThat(output.getAll()).contains("Discarded 3 of 4 posts with invalid data. Ids: [abc, 2, 3]");
  }

  @Test
  void keepsPostsWithoutAValidCover() {
    givenMedia(
        """
        {"id": "1", "media_type": "IMAGE", "permalink": "https://www.instagram.com/p/AbC/",
         "media_url": "https://evilfbcdn.net/img.jpg", "timestamp": "2026-10-13T10:00:00+0000"}
        """,
        """
        {"id": "2", "media_type": "VIDEO", "permalink": "https://www.instagram.com/reel/AbC/",
         "timestamp": "2026-10-12T10:00:00+0000"}
        """);

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .extracting(InstagramPost::id, InstagramPost::previewImageUrl)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("1", null),
            org.assertj.core.groups.Tuple.tuple("2", null));
  }

  @Test
  void returnsFewerThanThreePostsWhenThereAreNoMore() {
    givenMedia(
        post("1", "IMAGE", "2026-10-13T10:00:00+0000"),
        post("2", "STORY", "2026-10-13T10:00:00+0000"));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME))
        .extracting(InstagramPost::id)
        .containsExactly("1");
  }

  @Test
  void aProfileWithoutMediaReturnsNoPosts() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson("{\"business_discovery\": {\"username\": \"kim_angel\"}}")));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME)).isEmpty();
  }

  @Test
  void rejectsAResponseForAnotherProfile() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(
                okJson(
                    "{\"business_discovery\": {\"username\": \"someone_else\","
                        + " \"media\": {\"data\": []}}}")));

    assertThatThrownBy(() -> instagramAdapter.fetchLatestPosts(USERNAME))
        .isInstanceOf(RemoteException.class)
        .hasMessage("[instagram_graph_api] unexpected profile");
  }

  @Test
  void rejectsAResponseWithoutBusinessDiscovery() {
    WIRE_MOCK.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("{\"id\": \"1\"}")));

    assertThatThrownBy(() -> instagramAdapter.fetchLatestPosts(USERNAME))
        .isInstanceOf(RemoteException.class);
  }

  @Test
  void rejectsAProviderErrorInASuccessfulResponse() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson("{\"error\": {\"message\": \"x\", \"type\": \"GraphMethodException\", \"code\": 100}}")));

    assertThatThrownBy(() -> instagramAdapter.fetchLatestPosts(USERNAME))
        .isInstanceOf(RemoteException.class)
        .hasMessage("[instagram_graph_api] provider error");
  }

  @Test
  void rejectsAnInvalidUsernameWithoutCallingTheProvider() {
    assertThatThrownBy(() -> instagramAdapter.fetchLatestPosts("kim_angel){id}"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(WIRE_MOCK.getAllServeEvents()).isEmpty();
  }

  @Test
  void retriesTransientErrorsUntilSuccess() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .inScenario("retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("second"));
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .inScenario("retry")
            .whenScenarioStateIs("second")
            .willReturn(aResponse().withStatus(429))
            .willSetStateTo("recovered"));
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .inScenario("retry")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson(response(post("1", "IMAGE", "2026-10-13T10:00:00+0000")))));

    assertThat(instagramAdapter.fetchLatestPosts(USERNAME)).hasSize(1);
    WIRE_MOCK.verify(3, getRequestedFor(urlPathEqualTo(PATH)));
  }

  @Test
  void retriesTimeoutsUpToThreeAttempts() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(okJson(response()).withFixedDelay(1_000)));

    assertThatThrownBy(() -> instagramAdapter.fetchLatestPosts(USERNAME))
        .isInstanceOfSatisfying(
            RemoteException.class, e -> assertThat(e.getStatusCode()).isEqualTo(504));
    WIRE_MOCK.verify(3, getRequestedFor(urlPathEqualTo(PATH)));
  }

  @Test
  void anExpiredTokenIsAnIdentifiableErrorWithoutRetries(final CapturedOutput output) {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {"error": {"message": "Error validating access token: META-SECRET-DETAIL",
                          "type": "OAuthException", "code": 190, "error_subcode": 463,
                          "fbtrace_id": "TRACE"}}
                        """)));

    assertThatThrownBy(() -> instagramAdapter.fetchLatestPosts(USERNAME))
        .isInstanceOf(RemoteException.class)
        .hasMessage("[instagram_graph_api] access token invalid or expired");
    WIRE_MOCK.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
    assertThat(output.getAll())
        .contains("Access token rejected (code 190, subcode 463): renew INSTAGRAM_ACCESS_TOKEN")
        .doesNotContain("META-SECRET-DETAIL")
        .doesNotContain(TOKEN);
  }

  @Test
  void missingPermissionsIsAnIdentifiableError() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo(PATH))
            .willReturn(
                aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"error\": {\"message\": \"x\", \"type\": \"OAuthException\", \"code\": 10}}")));

    assertThatThrownBy(() -> instagramAdapter.fetchLatestPosts(USERNAME))
        .isInstanceOf(RemoteException.class)
        .hasMessage("[instagram_graph_api] missing permissions");
  }

  @Test
  void neverLogsTheTokenNorSignedUrls(final CapturedOutput output) {
    givenMedia(
        """
        {"id": "bad", "media_type": "IMAGE", "permalink": "https://www.instagram.com/p/AbC/",
         "media_url": "%s", "timestamp": "2026-10-13T10:00:00+0000"}
        """
            .formatted(SIGNED_IMAGE),
        post("1", "IMAGE", "2026-10-13T10:00:00+0000"));
    instagramAdapter.fetchLatestPosts(USERNAME);

    WIRE_MOCK.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500)));
    assertThatThrownBy(() -> instagramAdapter.fetchLatestPosts(USERNAME))
        .isInstanceOf(RemoteException.class);

    assertThat(output.getAll())
        .contains("instagram_graph_api")
        .doesNotContain(TOKEN)
        .doesNotContain("SIGNATURE");
  }

  private static void givenMedia(final String... media) {
    WIRE_MOCK.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(response(media))));
  }

  private static String response(final String... media) {
    return """
        {"business_discovery": {"username": "kim_angel", "media": {"data": [%s]}},
         "id": "%s"}
        """
        .formatted(Stream.of(media).collect(Collectors.joining(",")), ACCOUNT_ID);
  }

  private static String post(final String id, final String mediaType, final String timestamp) {
    return """
        {"id": "%s", "media_type": "%s", "permalink": "https://www.instagram.com/p/C%s/",
         "media_url": "%s", "timestamp": "%s"}
        """
        .formatted(id, mediaType, id, SIGNED_IMAGE, timestamp);
  }
}
