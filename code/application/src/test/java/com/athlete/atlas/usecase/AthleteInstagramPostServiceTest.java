package com.athlete.atlas.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.athlete.atlas.config.InstagramSelectionProperties;
import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.instagram.InstagramMediaType;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.port.output.InstagramPort;
import com.athlete.atlas.port.output.InstagramPostSelectionRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The selection rules shared with YouTube are covered in depth by AthleteYouTubeVideoServiceTest;
 * this test checks the Instagram wiring and the configuration validation.
 */
class AthleteInstagramPostServiceTest {

  private static final String ATHLETE = "kim-angel";

  private static final String USERNAME = "kim_angel";

  private static final Instant NOW = Instant.parse("2026-10-14T06:00:00Z");

  private InstagramPort instagramPort;

  private InMemoryRepository repository;

  private AthleteInstagramPostService service;

  @BeforeEach
  void setUp() {
    instagramPort = mock(InstagramPort.class);
    repository = new InMemoryRepository();
    service =
        new AthleteInstagramPostService(
            instagramPort,
            repository,
            new InstagramSelectionProperties(Map.of(ATHLETE, USERNAME), Duration.ofHours(12)),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void anAthleteWithoutProfileIsNotFound() {
    assertThatThrownBy(() -> service.listAthleteInstagramPosts("unknown"))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(instagramPort);
  }

  @Test
  void isUnavailableUntilTheFirstSuccessfulRefresh() {
    assertThatThrownBy(() -> service.listAthleteInstagramPosts(ATHLETE))
        .isInstanceOf(DataUnavailableException.class);
  }

  @Test
  void refreshesTheProfileOfTheAthleteAndServesFewerThanThreePosts() {
    when(instagramPort.fetchLatestPosts(USERNAME)).thenReturn(posts(2));

    service.refreshAll();

    final var result = service.listAthleteInstagramPosts(ATHLETE);
    assertThat(result.items()).isEqualTo(posts(2));
    assertThat(result.updatedAt()).isEqualTo(NOW);
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void aFailedRefreshKeepsThePreviousSelectionAsStale() {
    when(instagramPort.fetchLatestPosts(USERNAME))
        .thenReturn(posts(3))
        .thenThrow(
            new RemoteException("[instagram_graph_api] access token invalid or expired", 400, null));
    service.refreshAll();

    service.refreshAll();

    final var result = service.listAthleteInstagramPosts(ATHLETE);
    assertThat(result.items()).isEqualTo(posts(3));
    assertThat(result.availability()).isEqualTo(Availability.STALE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"kim_angel){id}", "kim angel", "", "a_username_longer_than_thirty_chars"})
  void rejectsInvalidUsernamesInTheConfiguration(final String username) {
    assertThatThrownBy(
            () -> new InstagramSelectionProperties(Map.of(ATHLETE, username), Duration.ofHours(12)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid Instagram username configured for athlete kim-angel");
  }

  private static List<InstagramPost> posts(final int count) {
    return IntStream.rangeClosed(1, count)
        .mapToObj(
            n ->
                new InstagramPost(
                    String.valueOf(n),
                    InstagramMediaType.IMAGE,
                    NOW.minus(Duration.ofDays(n)),
                    URI.create("https://www.instagram.com/p/C" + n + "/"),
                    null))
        .toList();
  }

  private static final class InMemoryRepository implements InstagramPostSelectionRepository {

    private final Map<String, SynchronizedSelection<InstagramPost>> selections = new HashMap<>();

    @Override
    public Optional<SynchronizedSelection<InstagramPost>> findByAthleteId(final String athleteId) {
      return Optional.ofNullable(selections.get(athleteId));
    }

    @Override
    public void save(final String athleteId, final SynchronizedSelection<InstagramPost> selection) {
      selections.put(athleteId, selection);
    }
  }
}
