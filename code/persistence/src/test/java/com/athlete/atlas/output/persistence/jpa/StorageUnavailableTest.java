package com.athlete.atlas.output.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.competition.CompetitionCatalog;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.output.persistence.content.CompetitionCatalogRepositoryAdapter;
import com.athlete.atlas.output.persistence.content.StoredContentMapper;
import com.athlete.atlas.output.persistence.content.StoredContentMapperImpl;
import com.athlete.atlas.output.persistence.content.YouTubeVideoSelectionRepositoryAdapter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Stops its own database to check that an unavailable database becomes a
 * StorageUnavailableException, which the application turns into a degraded response, and that the
 * synchronized content keeps being served and refreshed from memory. Not transactional, so that no
 * connection is held when the database stops.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  AthleteRepositoryAdapter.class,
  CompetitionParticipationRepositoryAdapter.class,
  HallOfFameRepositoryAdapter.class,
  YouTubeVideoSelectionRepositoryAdapter.class,
  CompetitionCatalogRepositoryAdapter.class,
  StoredContentMapperImpl.class
})
@TestPropertySource(properties = "spring.datasource.hikari.connection-timeout=500")
class StorageUnavailableTest {

  private static final Instant T0 = Instant.parse("2026-10-14T06:00:00Z");

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired private AthleteRepositoryAdapter athleteRepository;

  @Autowired private CompetitionParticipationRepositoryAdapter participationRepository;

  @Autowired private HallOfFameRepositoryAdapter hallOfFameRepository;

  @Autowired private YouTubeVideoSelectionRepositoryAdapter youTubeRepository;

  @Autowired private CompetitionCatalogRepositoryAdapter catalogRepository;

  @Autowired private StoredContentMapper storedContentMapper;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void anUnavailableDatabaseIsReportedAsStorageUnavailable() {
    assertThat(athleteRepository.findByCode("kim-angel")).isPresent();
    youTubeRepository.save("kim-angel", SynchronizedSelection.refreshedAt(List.of(), T0));

    POSTGRES.stop();

    assertThatThrownBy(() -> athleteRepository.findByCode("kim-angel"))
        .isInstanceOf(StorageUnavailableException.class);
    assertThatThrownBy(() -> athleteRepository.findAll())
        .isInstanceOf(StorageUnavailableException.class);
    assertThatThrownBy(() -> participationRepository.findConfirmedCompetitionIds("kim-angel"))
        .isInstanceOf(StorageUnavailableException.class);
    assertThatThrownBy(() -> hallOfFameRepository.findByAthleteCode("kim-angel"))
        .isInstanceOf(StorageUnavailableException.class);

    // Content that is not in memory cannot be read.
    assertThatThrownBy(() -> catalogRepository.find())
        .isInstanceOf(StorageUnavailableException.class);
    assertThatThrownBy(
            () ->
                new YouTubeVideoSelectionRepositoryAdapter(jdbcTemplate, storedContentMapper)
                    .findByAthleteId("kim-angel"))
        .isInstanceOf(StorageUnavailableException.class);

    // A refresh is served from memory although it cannot be stored.
    final var refreshed =
        SynchronizedSelection.<YouTubeVideo>refreshedAt(List.of(), T0.plusSeconds(3600));
    youTubeRepository.save("kim-angel", refreshed);
    assertThat(youTubeRepository.findByAthleteId("kim-angel")).contains(refreshed);

    final var catalog =
        new CompetitionCatalog(
            DateRange.seasonOf(LocalDate.of(2026, 10, 14)),
            SynchronizedSelection.refreshedAt(List.of(), T0));
    catalogRepository.save(catalog);
    assertThat(catalogRepository.find()).contains(catalog);
  }
}
