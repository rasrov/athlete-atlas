package com.athlete.atlas.output.persistence.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.CompetitionCatalog;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.model.instagram.InstagramMediaType;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Stores the synchronized content on a real PostgreSQL. A restart is simulated with a new adapter
 * instance, which has nothing in memory and reads the table.
 */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  CompetitionCatalogRepositoryAdapter.class,
  YouTubeVideoSelectionRepositoryAdapter.class,
  InstagramPostSelectionRepositoryAdapter.class,
  StoredContentMapperImpl.class
})
class ContentRepositoryAdaptersTest {

  private static final String ATHLETE = "kim-angel";

  private static final Instant T0 = Instant.parse("2026-10-14T06:00:00Z");

  private static final DateRange SEASON_2026 =
      new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 10, 1));

  private static final DateRange SEASON_2025 =
      new DateRange(LocalDate.of(2025, 10, 1), LocalDate.of(2026, 10, 1));

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired private CompetitionCatalogRepositoryAdapter catalogRepository;

  @Autowired private YouTubeVideoSelectionRepositoryAdapter youTubeRepository;

  @Autowired private InstagramPostSelectionRepositoryAdapter instagramRepository;

  @Autowired private StoredContentMapper storedContentMapper;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void aYouTubeSelectionSurvivesARestart() {
    final var selection = SynchronizedSelection.refreshedAt(List.of(video("a"), video("b")), T0);
    youTubeRepository.save(ATHLETE, selection);

    assertThat(restartedYouTubeRepository().findByAthleteId(ATHLETE)).contains(selection);
  }

  @Test
  void anInstagramSelectionKeepsItsNullFieldsAndItsFailedRefresh() {
    final var post =
        new InstagramPost(
            "17890000000000001",
            InstagramMediaType.CAROUSEL_ALBUM,
            T0,
            URI.create("https://www.instagram.com/p/C1/"),
            null);
    final var selection = SynchronizedSelection.refreshedAt(List.of(post), T0).withFailedRefresh();
    instagramRepository.save(ATHLETE, selection);

    assertThat(restartedInstagramRepository().findByAthleteId(ATHLETE)).contains(selection);
  }

  @Test
  void anEmptySelectionIsStoredAsValidData() {
    final var selection = SynchronizedSelection.<YouTubeVideo>refreshedAt(List.of(), T0);
    youTubeRepository.save(ATHLETE, selection);

    assertThat(restartedYouTubeRepository().findByAthleteId(ATHLETE)).contains(selection);
  }

  @Test
  void savingReplacesThePreviousSelection() {
    youTubeRepository.save(ATHLETE, SynchronizedSelection.refreshedAt(List.of(video("a")), T0));
    final var latest =
        SynchronizedSelection.refreshedAt(List.of(video("b")), T0.plusSeconds(3600));

    youTubeRepository.save(ATHLETE, latest);

    assertThat(restartedYouTubeRepository().findByAthleteId(ATHLETE)).contains(latest);
    assertThat(count("content_selection")).isEqualTo(1);
  }

  @Test
  void eachSourceHasItsOwnSelection() {
    youTubeRepository.save(ATHLETE, SynchronizedSelection.refreshedAt(List.of(video("a")), T0));

    assertThat(restartedInstagramRepository().findByAthleteId(ATHLETE)).isEmpty();
  }

  @Test
  void aSelectionOfAnUnknownAthleteIsOnlyKeptInMemory() {
    final var selection = SynchronizedSelection.refreshedAt(List.of(video("a")), T0);

    youTubeRepository.save("unknown", selection);

    assertThat(youTubeRepository.findByAthleteId("unknown")).contains(selection);
    assertThat(restartedYouTubeRepository().findByAthleteId("unknown")).isEmpty();
    assertThat(count("content_selection")).isZero();
  }

  @Test
  void anUnreadableSelectionIsIgnored() {
    jdbcTemplate.update(
        "INSERT INTO content_selection (athlete_id, source, items, updated_at, last_refresh_failed)"
            + " SELECT id, 'youtube', '[{\"publishedAt\": \"not a date\"}]', now(), false"
            + " FROM athlete WHERE code = ?",
        ATHLETE);

    assertThat(restartedYouTubeRepository().findByAthleteId(ATHLETE)).isEmpty();
  }

  @Test
  void anInstagramPostWithAnUnknownMediaTypeIsIgnored() {
    jdbcTemplate.update(
        "INSERT INTO content_selection (athlete_id, source, items, updated_at, last_refresh_failed)"
            + " SELECT id, 'instagram', '[{\"id\": \"1\", \"mediaType\": \"REEL\"}]', now(), false"
            + " FROM athlete WHERE code = ?",
        ATHLETE);

    assertThat(restartedInstagramRepository().findByAthleteId(ATHLETE)).isEmpty();
  }

  @Test
  void deletingAnAthleteDeletesItsSelections() {
    youTubeRepository.save(ATHLETE, SynchronizedSelection.refreshedAt(List.of(video("a")), T0));

    jdbcTemplate.update("DELETE FROM athlete WHERE code = ?", ATHLETE);

    assertThat(count("content_selection")).isZero();
  }

  @Test
  void rejectsAnUnknownSource() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO content_selection"
                        + " (athlete_id, source, items, updated_at, last_refresh_failed)"
                        + " SELECT id, 'tiktok', '[]', now(), false FROM athlete WHERE code = ?",
                    ATHLETE))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsItemsThatAreNotAList() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO content_selection"
                        + " (athlete_id, source, items, updated_at, last_refresh_failed)"
                        + " SELECT id, 'youtube', '{}', now(), false FROM athlete WHERE code = ?",
                    ATHLETE))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void theCatalogSurvivesARestartWithItsNullFields() {
    final var catalog =
        new CompetitionCatalog(
            SEASON_2026,
            SynchronizedSelection.refreshedAt(
                List.of(competition("ifbb-1", "Madrid"), competition("ifbb-2", null)), T0));
    catalogRepository.save(catalog);

    assertThat(restartedCatalogRepository().find()).contains(catalog);
  }

  @Test
  void theLatestSeasonIsServedAndThePreviousOnesAreKept() {
    final var previous =
        new CompetitionCatalog(
            SEASON_2025,
            SynchronizedSelection.refreshedAt(List.of(competition("ifbb-1", null)), T0));
    final var current =
        new CompetitionCatalog(
            SEASON_2026,
            SynchronizedSelection.refreshedAt(List.of(competition("ifbb-2", null)), T0)
                .withFailedRefresh());
    catalogRepository.save(current);
    catalogRepository.save(previous);

    assertThat(restartedCatalogRepository().find()).contains(current);
    assertThat(count("competition_catalog")).isEqualTo(2);
  }

  @Test
  void savingTheSameSeasonReplacesIt() {
    catalogRepository.save(
        new CompetitionCatalog(
            SEASON_2026,
            SynchronizedSelection.refreshedAt(List.of(competition("ifbb-1", null)), T0)));
    final var latest =
        new CompetitionCatalog(
            SEASON_2026,
            SynchronizedSelection.refreshedAt(
                List.of(competition("ifbb-2", null)), T0.plusSeconds(3600)));

    catalogRepository.save(latest);

    assertThat(restartedCatalogRepository().find()).contains(latest);
    assertThat(count("competition_catalog")).isEqualTo(1);
  }

  @Test
  void withoutStoredCatalogThereIsNothingToServe() {
    assertThat(restartedCatalogRepository().find()).isEmpty();
  }

  @Test
  void anUnreadableCatalogIsIgnored() {
    jdbcTemplate.update(
        "INSERT INTO competition_catalog"
            + " (season_start, season_end_exclusive, items, updated_at, last_refresh_failed)"
            + " VALUES ('2026-10-01', '2027-10-01', '[{\"startDate\": \"tomorrow\"}]', now(), false)");

    assertThat(restartedCatalogRepository().find()).isEmpty();
  }

  private YouTubeVideoSelectionRepositoryAdapter restartedYouTubeRepository() {
    return new YouTubeVideoSelectionRepositoryAdapter(jdbcTemplate, storedContentMapper);
  }

  private InstagramPostSelectionRepositoryAdapter restartedInstagramRepository() {
    return new InstagramPostSelectionRepositoryAdapter(jdbcTemplate, storedContentMapper);
  }

  private CompetitionCatalogRepositoryAdapter restartedCatalogRepository() {
    return new CompetitionCatalogRepositoryAdapter(jdbcTemplate, storedContentMapper);
  }

  private long count(final String table) {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  private static YouTubeVideo video(final String id) {
    return new YouTubeVideo(
        id,
        "Video " + id,
        T0.minusSeconds(86_400),
        URI.create("https://www.youtube.com/watch?v=" + id),
        URI.create("https://www.youtube-nocookie.com/embed/" + id),
        null);
  }

  private static AthleteCompetition competition(final String id, final String city) {
    return new AthleteCompetition(
        id,
        "Competition " + id,
        LocalDate.of(2027, 3, 1),
        LocalDate.of(2027, 3, 2),
        city,
        city == null ? null : "Spain",
        "Classic Physique",
        URI.create("https://ifbbpro.com/competition/" + id),
        id,
        null);
  }
}
