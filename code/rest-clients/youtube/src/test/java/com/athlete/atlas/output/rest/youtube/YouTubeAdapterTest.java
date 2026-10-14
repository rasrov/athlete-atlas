package com.athlete.atlas.output.rest.youtube;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
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
import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.output.rest.youtube.client.YouTubeDataClient;
import com.athlete.atlas.output.rest.youtube.config.YouTubeClientConfiguration;
import com.athlete.atlas.output.rest.youtube.mapper.YouTubeVideoMapperImpl;
import com.athlete.atlas.service.InvokerServiceImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
 * Exercises the whole selection against simulated YouTube responses, with the real generated
 * client, JSON mapping and retries (waits shortened to 1ms).
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
    classes = YouTubeAdapterTest.TestApplication.class,
    properties = {
      "rest-clients.youtube.api-key=" + YouTubeAdapterTest.API_KEY,
      "rest-clients.youtube.connect-timeout=1s",
      "rest-clients.youtube.read-timeout=2s",
      "rest-clients.youtube.page-size=50",
      "rest-clients.youtube.max-pages=4",
      "rest-clients.youtube.selection-size=6",
      "resilience4j.retry.instances.youTubeDataApi.max-attempts=3",
      "resilience4j.retry.instances.youTubeDataApi.wait-duration=1ms",
      "resilience4j.retry.instances.youTubeDataApi.retry-exceptions="
          + "org.springframework.web.client.HttpClientErrorException$TooManyRequests,"
          + "org.springframework.web.client.HttpServerErrorException$InternalServerError,"
          + "org.springframework.web.client.HttpServerErrorException$BadGateway,"
          + "org.springframework.web.client.HttpServerErrorException$ServiceUnavailable,"
          + "org.springframework.web.client.HttpServerErrorException$GatewayTimeout,"
          + "org.springframework.web.client.ResourceAccessException"
    })
class YouTubeAdapterTest {

  static final String API_KEY = "TEST-SECRET-API-KEY-1234";

  private static final String CHANNEL_ID = "UCEqAjtwYKSwKDN4BENhvc6A";

  private static final String UPLOADS = "UUEqAjtwYKSwKDN4BENhvc6A";

  private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

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
    YouTubeClientConfiguration.class,
    YouTubeDataClient.class,
    YouTubeAdapter.class,
    YouTubeVideoMapperImpl.class,
    InvokerServiceImpl.class
  })
  static class TestApplication {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }

  @DynamicPropertySource
  static void youTubeBaseUrl(final DynamicPropertyRegistry registry) {
    registry.add("rest-clients.youtube.base-url", WIRE_MOCK::baseUrl);
  }

  @Autowired private YouTubeAdapter youTubeAdapter;

  @BeforeEach
  void resetStubs() {
    WIRE_MOCK.resetAll();
  }

  @AfterAll
  static void stopWireMock() {
    WIRE_MOCK.stop();
  }

  @Test
  void selectsTheSixLatestVideosFromTheFirstPageWithThreeRequests() {
    givenChannel();
    givenPlaylistPage(null, "NEXT", 1, 2, 3, 4, 5, 6, 7, 8);
    givenVideos(ids(1, 2, 3, 4, 5, 6, 7, 8), IntStream.rangeClosed(1, 8).mapToObj(YouTubeAdapterTest::video));

    final var videos = youTubeAdapter.fetchLatestVideos(CHANNEL_ID);

    assertThat(videos).extracting(YouTubeVideo::id).containsExactly(ids(8, 7, 6, 5, 4, 3));
    assertThat(WIRE_MOCK.getAllServeEvents()).hasSize(3);
  }

  @Test
  void requestsMorePagesWhenVideosAreDiscardedFollowingTheToken() {
    givenChannel();
    givenPlaylistPage(null, "PAGE-2", 1, 2, 3, 4, 5, 6);
    givenVideos(
        ids(1, 2, 3, 4, 5, 6),
        Stream.of(
            video(1),
            video(2),
            video(3),
            privacy(4, "private"),
            privacy(5, "private"),
            privacy(6, "unlisted")));
    givenPlaylistPage("PAGE-2", null, 7, 8, 9);
    givenVideos(ids(7, 8, 9), Stream.of(video(7), video(8), video(9)));

    final var videos = youTubeAdapter.fetchLatestVideos(CHANNEL_ID);

    assertThat(videos).extracting(YouTubeVideo::id).containsExactly(ids(9, 8, 7, 3, 2, 1));
    WIRE_MOCK.verify(
        getRequestedFor(urlPathEqualTo("/playlistItems")).withQueryParam("pageToken", absent()));
    WIRE_MOCK.verify(
        getRequestedFor(urlPathEqualTo("/playlistItems"))
            .withQueryParam("pageToken", equalTo("PAGE-2")));
  }

  @Test
  void returnsTheVideosFoundWhenThePlaylistEndsBeforeSix() {
    givenChannel();
    givenPlaylistPage(null, null, 1, 2);
    givenVideos(ids(1, 2), Stream.of(video(1), video(2)));

    assertThat(youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .extracting(YouTubeVideo::id)
        .containsExactly(ids(2, 1));
  }

  @Test
  void anEmptyChannelReturnsNoVideosWithoutRequestingDetails() {
    givenChannel();
    givenPlaylistPage(null, null);

    assertThat(youTubeAdapter.fetchLatestVideos(CHANNEL_ID)).isEmpty();
    WIRE_MOCK.verify(0, getRequestedFor(urlPathEqualTo("/videos")));
  }

  @Test
  void stopsAtThePageLimitAndReturnsWhatWasFound() {
    givenChannel();
    givenPlaylistPage(null, "PAGE-2", 1);
    givenPlaylistPage("PAGE-2", "PAGE-3", 2);
    givenPlaylistPage("PAGE-3", "PAGE-4", 3);
    givenPlaylistPage("PAGE-4", "PAGE-5", 4);
    for (int n = 1; n <= 4; n++) {
      givenVideos(ids(n), Stream.of(video(n)));
    }

    assertThat(youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .extracting(YouTubeVideo::id)
        .containsExactly(ids(4, 3, 2, 1));
    WIRE_MOCK.verify(4, getRequestedFor(urlPathEqualTo("/playlistItems")));
  }

  @Test
  void aRepeatedPageTokenIsAProviderError() {
    givenChannel();
    givenPlaylistPage(null, "PAGE-2", 1);
    givenPlaylistPage("PAGE-2", "PAGE-2", 2);
    givenVideos(ids(1), Stream.of(video(1)));
    givenVideos(ids(2), Stream.of(video(2)));

    assertThatThrownBy(() -> youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .isInstanceOf(RemoteException.class);
  }

  @Test
  void excludesIneligibleVideos() {
    givenChannel();
    givenPlaylistPage(null, null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
    givenVideos(
        ids(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
        Stream.of(
            video(1),
            privacy(2, "private"),
            privacy(3, "unlisted"),
            json(id(4), CHANNEL_ID, "Video 4", published(4), "none", "public", "false"),
            json(id(5), CHANNEL_ID, "Video 5", published(5), "none", "public", "null"),
            json(id(6), CHANNEL_ID, "Video 6", "2026-10-15T00:00:00Z", "none", "public", "true"),
            json(id(7), CHANNEL_ID, "Video 7", published(7), "live", "public", "true"),
            json(id(8), CHANNEL_ID, "Video 8", published(8), "upcoming", "public", "true"),
            json(id(9), "UCanotherChannel000000", "Video 9", published(9), "none", "public", "true"),
            json(id(10), CHANNEL_ID, " ", published(10), "none", "public", "true"),
            json("short-id", CHANNEL_ID, "Video 11", published(11), "none", "public", "true")));

    assertThat(youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .extracting(YouTubeVideo::id)
        .containsExactly(id(1));
  }

  @Test
  void requestsEachVideoOnceAndRemovesDuplicates() {
    givenChannel();
    givenPlaylistPage(null, "PAGE-2", 1, 1, 2);
    givenVideos(ids(1, 2), Stream.of(video(1), video(2), video(2)));
    givenPlaylistPage("PAGE-2", null, 2, 3);
    givenVideos(ids(3), Stream.of(video(3)));

    assertThat(youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .extracting(YouTubeVideo::id)
        .containsExactly(ids(3, 2, 1));
  }

  @Test
  void ordersByPublicationDateDescendingThenById() {
    givenChannel();
    givenPlaylistPage(null, null, 2, 1, 3);
    givenVideos(
        ids(2, 1, 3),
        Stream.of(
            json(id(2), CHANNEL_ID, "Video 2", "2026-10-01T10:00:00Z", "none", "public", "true"),
            json(id(1), CHANNEL_ID, "Video 1", "2026-10-01T10:00:00Z", "none", "public", "true"),
            json(id(3), CHANNEL_ID, "Video 3", "2026-09-01T10:00:00Z", "none", "public", "true")));

    assertThat(youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .extracting(YouTubeVideo::id)
        .containsExactly(ids(1, 2, 3));
  }

  @Test
  void mapsDatesAndUrls() {
    givenChannel();
    givenPlaylistPage(null, null, 1);
    givenVideos(ids(1), Stream.of(video(1)));

    assertThat(youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .containsExactly(
            new YouTubeVideo(
                id(1),
                "Video 1",
                Instant.parse(published(1)),
                URI.create("https://www.youtube.com/watch?v=" + id(1)),
                URI.create("https://www.youtube-nocookie.com/embed/" + id(1)),
                URI.create("https://i.ytimg.com/vi/" + id(1) + "/hqdefault.jpg")));
  }

  @Test
  void aChannelWithoutUploadsPlaylistIsAProviderError() {
    WIRE_MOCK.stubFor(get(urlPathEqualTo("/channels")).willReturn(okJson("{\"items\": []}")));

    assertThatThrownBy(() -> youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .isInstanceOf(RemoteException.class);
    WIRE_MOCK.verify(0, getRequestedFor(urlPathEqualTo("/playlistItems")));
  }

  @Test
  void retriesTransientErrorsUntilSuccess() {
    givenChannel();
    givenPlaylistPage(null, null, 1);
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/videos"))
            .inScenario("retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("second"));
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/videos"))
            .inScenario("retry")
            .whenScenarioStateIs("second")
            .willReturn(aResponse().withStatus(429))
            .willSetStateTo("recovered"));
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/videos"))
            .inScenario("retry")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson(items(Stream.of(video(1))))));

    assertThat(youTubeAdapter.fetchLatestVideos(CHANNEL_ID)).hasSize(1);
    WIRE_MOCK.verify(3, getRequestedFor(urlPathEqualTo("/videos")));
  }

  @Test
  void givesUpAfterThreeAttempts() {
    WIRE_MOCK.stubFor(get(urlPathEqualTo("/channels")).willReturn(aResponse().withStatus(500)));

    assertThatThrownBy(() -> youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .isInstanceOf(RemoteException.class);
    WIRE_MOCK.verify(3, getRequestedFor(urlPathEqualTo("/channels")));
  }

  @Test
  void doesNotRetryOtherClientErrors() {
    WIRE_MOCK.stubFor(get(urlPathEqualTo("/channels")).willReturn(aResponse().withStatus(403)));

    assertThatThrownBy(() -> youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .isInstanceOf(RemoteException.class);
    WIRE_MOCK.verify(1, getRequestedFor(urlPathEqualTo("/channels")));
  }

  @Test
  void sendsTheApiKeyOnlyInTheHeader() {
    givenChannel();
    givenPlaylistPage(null, null, 1);
    givenVideos(ids(1), Stream.of(video(1)));

    youTubeAdapter.fetchLatestVideos(CHANNEL_ID);

    WIRE_MOCK.verify(
        3,
        anyRequestedFor(anyUrl())
            .withHeader("X-goog-api-key", equalTo(API_KEY))
            .withQueryParam("key", absent()));
  }

  @Test
  void theApiKeyNeverAppearsInErrorsOrLogs(final CapturedOutput output) {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/channels"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    assertThatThrownBy(() -> youTubeAdapter.fetchLatestVideos(CHANNEL_ID))
        .isInstanceOf(RemoteException.class)
        .satisfies(
            e -> {
              for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                assertThat(String.valueOf(cause.getMessage())).doesNotContain(API_KEY);
              }
            });
    WIRE_MOCK.verify(3, getRequestedFor(urlPathEqualTo("/channels")));
    assertThat(output.getAll()).contains("youtube_data_api").doesNotContain(API_KEY);
  }

  private static void givenChannel() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/channels"))
            .withQueryParam("part", equalTo("contentDetails"))
            .withQueryParam("id", equalTo(CHANNEL_ID))
            .willReturn(
                okJson(
                    """
                    {"items": [{"id": "%s",
                      "contentDetails": {"relatedPlaylists": {"uploads": "%s"}}}]}
                    """
                        .formatted(CHANNEL_ID, UPLOADS))));
  }

  private static void givenPlaylistPage(
      final String pageToken, final String nextPageToken, final int... videoNumbers) {
    final var items =
        Arrays.stream(videoNumbers)
            .mapToObj(n -> "{\"contentDetails\": {\"videoId\": \"%s\"}}".formatted(id(n)))
            .collect(Collectors.joining(","));
    final var next = nextPageToken == null ? "" : "\"nextPageToken\": \"%s\",".formatted(nextPageToken);
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/playlistItems"))
            .withQueryParam("part", equalTo("contentDetails"))
            .withQueryParam("playlistId", equalTo(UPLOADS))
            .withQueryParam("maxResults", equalTo("50"))
            .withQueryParam("pageToken", pageToken == null ? absent() : equalTo(pageToken))
            .willReturn(okJson("{%s \"items\": [%s]}".formatted(next, items))));
  }

  private static void givenVideos(
      final String[] requestedIds, final Stream<String> videos) {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/videos"))
            .withQueryParam("part", equalTo("snippet,status"))
            .withQueryParam("id", equalTo(String.join(",", requestedIds)))
            .willReturn(okJson(items(videos))));
  }

  private static String items(final Stream<String> videos) {
    return "{\"items\": [%s]}".formatted(videos.collect(Collectors.joining(",")));
  }

  /** Eligible video published at hour n of the day before NOW, so a higher n is more recent. */
  private static String video(final int n) {
    return json(id(n), CHANNEL_ID, "Video " + n, published(n), "none", "public", "true");
  }

  private static String privacy(final int n, final String privacyStatus) {
    return json(id(n), CHANNEL_ID, "Video " + n, published(n), "none", privacyStatus, "true");
  }

  private static String json(
      final String id,
      final String channelId,
      final String title,
      final String publishedAt,
      final String liveBroadcastContent,
      final String privacyStatus,
      final String embeddable) {
    return """
        {"id": "%s",
         "snippet": {"channelId": "%s", "title": "%s", "publishedAt": "%s",
           "liveBroadcastContent": "%s",
           "thumbnails": {"high": {"url": "https://i.ytimg.com/vi/%s/hqdefault.jpg"}}},
         "status": {"privacyStatus": "%s", "embeddable": %s}}
        """
        .formatted(
            id, channelId, title, publishedAt, liveBroadcastContent, id, privacyStatus, embeddable);
  }

  private static String published(final int n) {
    return "2026-10-13T%02d:00:00Z".formatted(n);
  }

  private static String id(final int n) {
    return "video%06d".formatted(n);
  }

  private static String[] ids(final int... numbers) {
    return Arrays.stream(numbers).mapToObj(YouTubeAdapterTest::id).toArray(String[]::new);
  }
}
