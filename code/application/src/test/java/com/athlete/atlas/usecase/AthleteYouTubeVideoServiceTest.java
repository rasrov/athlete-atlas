package com.athlete.atlas.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.athlete.atlas.config.YouTubeSelectionProperties;
import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.athlete.Athlete;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.port.output.YouTubePort;
import com.athlete.atlas.port.output.YouTubeVideoSelectionRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
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

  private FakeAthleteRepository athletes;

  private MutableClock clock;

  private AthleteYouTubeVideoService service;

  @BeforeEach
  void setUp() {
    youTubePort = mock(YouTubePort.class);
    repository = new InMemoryRepository();
    clock = new MutableClock(T0);
    athletes = new FakeAthleteRepository(new Athlete(ATHLETE, CHANNEL, null));
    service =
        new AthleteYouTubeVideoService(
            youTubePort,
            repository,
            athletes,
            new YouTubeSelectionProperties(Duration.ofHours(12)),
            clock);
  }

  @Test
  void anUnknownAthleteIsNotFound() {
    assertThatThrownBy(() -> service.listAthleteYouTubeVideos("unknown"))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(youTubePort);
  }

  @Test
  void anAthleteWithoutChannelIsNotFoundAndIsNotRefreshed() {
    athletes.replaceAll(new Athlete(ATHLETE, CHANNEL, null), new Athlete("no-channel", null, null));

    service.refreshAll();

    verify(youTubePort).fetchLatestVideos(CHANNEL);
    verifyNoMoreInteractions(youTubePort);
    assertThatThrownBy(() -> service.listAthleteYouTubeVideos("no-channel"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void anUnavailableStorageOnReadIsPropagated() {
    athletes.setAvailable(false);

    assertThatThrownBy(() -> service.listAthleteYouTubeVideos(ATHLETE))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void anUnavailableStorageOnRefreshKeepsThePreviousSelectionAsStale() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(6));
    service.refreshAll();
    athletes.setAvailable(false);

    service.refreshAll();

    verify(youTubePort, times(1)).fetchLatestVideos(CHANNEL);
    athletes.setAvailable(true);
    final var result = service.listAthleteYouTubeVideos(ATHLETE);
    assertThat(result.items()).isEqualTo(videos(6));
    assertThat(result.updatedAt()).isEqualTo(T0);
    assertThat(result.availability()).isEqualTo(Availability.STALE);
  }

  @Test
  void anUnavailableStorageOnTheFirstRefreshLeavesTheDataUnavailable() {
    athletes.setAvailable(false);

    service.refreshAll();

    verifyNoInteractions(youTubePort);
    athletes.setAvailable(true);
    assertThatThrownBy(() -> service.listAthleteYouTubeVideos(ATHLETE))
        .isInstanceOf(DataUnavailableException.class);
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
    assertThat(result.items()).isEqualTo(videos(6));
    assertThat(result.updatedAt()).isEqualTo(T0);
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void fewerThanSixVideosIsAValidSelection() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(2));

    service.refreshAll();

    final var result = service.listAthleteYouTubeVideos(ATHLETE);
    assertThat(result.items()).hasSize(2);
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void anEmptyChannelIsAValidSelection() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(List.of());

    service.refreshAll();

    assertThat(service.listAthleteYouTubeVideos(ATHLETE).items()).isEmpty();
  }

  @Test
  void theLatestSuccessfulRefreshReplacesTheSelectionWhateverItsSize() {
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(6)).thenReturn(videos(5));
    service.refreshAll();
    clock.advance(Duration.ofHours(6));

    service.refreshAll();

    final var result = service.listAthleteYouTubeVideos(ATHLETE);
    assertThat(result.items()).isEqualTo(videos(5));
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
    assertThat(result.items()).isEqualTo(videos(6));
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
    assertThat(result.items()).isEqualTo(videos(3));
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
    athletes.replaceAll(new Athlete("failing", "UCfailing", null), new Athlete(ATHLETE, CHANNEL, null));
    when(youTubePort.fetchLatestVideos("UCfailing"))
        .thenThrow(new RemoteException("[youtube_data_api] upstream error", 503, null));
    when(youTubePort.fetchLatestVideos(CHANNEL)).thenReturn(videos(6));

    service.refreshAll();

    assertThat(service.listAthleteYouTubeVideos(ATHLETE).items()).hasSize(6);
    assertThatThrownBy(() -> service.listAthleteYouTubeVideos("failing"))
        .isInstanceOf(DataUnavailableException.class);
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

    private final Map<String, SynchronizedSelection<YouTubeVideo>> selections = new HashMap<>();

    @Override
    public Optional<SynchronizedSelection<YouTubeVideo>> findByAthleteId(final String athleteId) {
      return Optional.ofNullable(selections.get(athleteId));
    }

    @Override
    public void save(final String athleteId, final SynchronizedSelection<YouTubeVideo> selection) {
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
