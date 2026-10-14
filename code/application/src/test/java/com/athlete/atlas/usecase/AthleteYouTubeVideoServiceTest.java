package com.athlete.atlas.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.athlete.atlas.config.YouTubeSelectionProperties;
import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.model.youtube.YouTubeVideoSelection;
import com.athlete.atlas.port.output.YouTubePort;
import com.athlete.atlas.port.output.YouTubeVideoSelectionRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AthleteYouTubeVideoServiceTest {

  private static final String ATHLETE = "kim-angel";

  private static final String CHANNEL = "UCEqAjtwYKSwKDN4BENhvc6A";

  private static final Instant T0 = Instant.parse("2026-10-14T06:00:00Z");

  private YouTubePort youTubePort;

  private InMemoryRepository repository;

  private MutableClock clock;

  private AthleteYouTubeVideoService service;

  @BeforeEach
  void setUp() {
    youTubePort = mock(YouTubePort.class);
    repository = new InMemoryRepository();
    clock = new MutableClock(T0);
    service = serviceWithChannels(Map.of(ATHLETE, CHANNEL));
  }

  @Test
  void anAthleteWithoutChannelIsNotFound() {
    assertThatThrownBy(() -> service.listAthleteYouTubeVideos("unknown"))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(youTubePort);
  }

  @Test
  void isUnavailableUntilTheFirstSuccessfulRefresh() {
    assertThatThrownBy(() -> service.listAthleteYouTubeVideos(ATHLETE))
        .isInstanceOf(DataUnavailableException.class);
  }

  @Test
  void servesTheRefreshedVideosAsAvailable() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(6));

    service.refreshAll();

    final var result = service.listAthleteYouTubeVideos(ATHLETE);
    assertThat(result.athleteId()).isEqualTo(ATHLETE);
    assertThat(result.videos()).isEqualTo(videos(6));
    assertThat(result.updatedAt()).isEqualTo(T0);
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void fewerThanSixVideosIsAValidSelection() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(2));

    service.refreshAll();

    final var result = service.listAthleteYouTubeVideos(ATHLETE);
    assertThat(result.videos()).hasSize(2);
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void anEmptyChannelIsAValidSelection() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(List.of());

    service.refreshAll();

    assertThat(service.listAthleteYouTubeVideos(ATHLETE).videos()).isEmpty();
  }

  @Test
  void theLatestSuccessfulRefreshReplacesTheSelectionWhateverItsSize() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(6)).thenReturn(videos(5));
    service.refreshAll();
    clock.advance(Duration.ofHours(6));

    service.refreshAll();

    final var result = service.listAthleteYouTubeVideos(ATHLETE);
    assertThat(result.videos()).isEqualTo(videos(5));
    assertThat(result.updatedAt()).isEqualTo(T0.plus(Duration.ofHours(6)));
  }

  @Test
  void aFailedRefreshKeepsThePreviousSelectionAsStale() {
    when(youTubePort.fetchLatestVideos(CHANNEL))
        .thenReturn(videos(6))
        .thenThrow(new RemoteException("[youtube_data_api] upstream error", 503, null));
    service.refreshAll();
    clock.advance(Duration.ofHours(6));

    service.refreshAll();

    final var result = service.listAthleteYouTubeVideos(ATHLETE);
    assertThat(result.videos()).isEqualTo(videos(6));
    assertThat(result.updatedAt()).isEqualTo(T0);
    assertThat(result.availability()).isEqualTo(Availability.STALE);
  }

  @Test
  void anUnexpectedErrorAlsoKeepsThePreviousSelection() {
    when(youTubePort.fetchLatestVideos(CHANNEL))
        .thenReturn(videos(3))
        .thenThrow(new IllegalStateException("bug"));
    service.refreshAll();

    service.refreshAll();

    final var result = service.listAthleteYouTubeVideos(ATHLETE);
    assertThat(result.videos()).isEqualTo(videos(3));
    assertThat(result.availability()).isEqualTo(Availability.STALE);
  }

  @Test
  void aFailedFirstRefreshLeavesTheDataUnavailable() {
    when(youTubePort.fetchLatestVideos(CHANNEL))
        .thenThrow(new RemoteException("[youtube_data_api] upstream error", 503, null));

    service.refreshAll();

    assertThatThrownBy(() -> service.listAthleteYouTubeVideos(ATHLETE))
        .isInstanceOf(DataUnavailableException.class);
  }

  @Test
  void aSuccessfulRefreshAfterAFailureIsAvailableAgain() {
    when(youTubePort.fetchLatestVideos(CHANNEL))
        .thenReturn(videos(6))
        .thenThrow(new RemoteException("[youtube_data_api] upstream error", 503, null))
        .thenReturn(videos(6));
    service.refreshAll();
    service.refreshAll();

    service.refreshAll();

    assertThat(service.listAthleteYouTubeVideos(ATHLETE).availability())
        .isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void aSelectionOlderThanTheFreshnessThresholdIsStale() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(6));
    service.refreshAll();

    clock.advance(Duration.ofHours(12));
    assertThat(service.listAthleteYouTubeVideos(ATHLETE).availability())
        .isEqualTo(Availability.AVAILABLE);

    clock.advance(Duration.ofSeconds(1));
    assertThat(service.listAthleteYouTubeVideos(ATHLETE).availability())
        .isEqualTo(Availability.STALE);
  }

  @Test
  void aFailingAthleteDoesNotStopTheRefreshOfTheOthers() {
    final var channels = new LinkedHashMap<String, String>();
    channels.put("failing", "UCfailing");
    channels.put(ATHLETE, CHANNEL);
    service = serviceWithChannels(channels);
    when(youTubePort.fetchLatestVideos("UCfailing"))
        .thenThrow(new RemoteException("[youtube_data_api] upstream error", 503, null));
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(6));

    service.refreshAll();

    assertThat(service.listAthleteYouTubeVideos(ATHLETE).videos()).hasSize(6);
    assertThatThrownBy(() -> service.listAthleteYouTubeVideos("failing"))
        .isInstanceOf(DataUnavailableException.class);
  }

  private AthleteYouTubeVideoService serviceWithChannels(final Map<String, String> channels) {
    return new AthleteYouTubeVideoService(
        youTubePort,
        repository,
        new YouTubeSelectionProperties(channels, Duration.ofHours(12)),
        clock);
  }

  private static List<YouTubeVideo> videos(final int count) {
    return IntStream.rangeClosed(1, count)
        .mapToObj(
            n -> {
              final var id = "video%06d".formatted(n);
              return new YouTubeVideo(
                  id,
                  "Video " + n,
                  T0.minus(Duration.ofDays(n)),
                  URI.create("https://www.youtube.com/watch?v=" + id),
                  URI.create("https://www.youtube-nocookie.com/embed/" + id),
                  null);
            })
        .toList();
  }

  private static final class InMemoryRepository implements YouTubeVideoSelectionRepository {

    private final Map<String, YouTubeVideoSelection> selections = new HashMap<>();

    @Override
    public Optional<YouTubeVideoSelection> findByAthleteId(final String athleteId) {
      return Optional.ofNullable(selections.get(athleteId));
    }

    @Override
    public void save(final String athleteId, final YouTubeVideoSelection selection) {
      selections.put(athleteId, selection);
    }
  }

  private static final class MutableClock extends Clock {

    private Instant now;

    private MutableClock(final Instant now) {
      this.now = now;
    }

    void advance(final Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(final ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
