package com.athlete.atlas.output.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.athlete.atlas.model.athlete.Athlete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs the Flyway migrations on a real PostgreSQL (not H2: the regex checks and sequences behave
 * differently) and validates the entities against them. Skipped when Docker is not available.
 */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// @DataJpaTest disables caching by default; the adapters are tested with the real provider.
@AutoConfigureCache(cacheProvider = CacheType.CAFFEINE)
@Import({
  AthleteRepositoryAdapter.class,
  CompetitionParticipationRepositoryAdapter.class,
  PersistenceAdaptersTest.CachingConfiguration.class
})
@TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.cache.cache-names=athletes,confirmedCompetitions"
    })
class PersistenceAdaptersTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired private AthleteRepositoryAdapter athleteRepository;

  @Autowired private CompetitionParticipationRepositoryAdapter participationRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private CacheManager cacheManager;

  @TestConfiguration
  @EnableCaching
  static class CachingConfiguration {}

  @BeforeEach
  void clearCaches() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
  }

  @Test
  void theMigrationsCreateTheInitialAthleteWithItsAccounts() {
    assertThat(athleteRepository.findByCode("kim-angel"))
        .contains(new Athlete("kim-angel", "UCEqAjtwYKSwKDN4BENhvc6A", "kim_angel"));
  }

  @Test
  void findAllReturnsEveryAthleteOrderedByCode() {
    insertAthlete("another-athlete");

    assertThat(athleteRepository.findAll())
        .containsExactly(
            new Athlete("another-athlete", null, null),
            new Athlete("kim-angel", "UCEqAjtwYKSwKDN4BENhvc6A", "kim_angel"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"UCshort", "XXEqAjtwYKSwKDN4BENhvc6A", "UCEqAjtwYKSwKDN4BENhvc6!"})
  void rejectsAnInvalidYouTubeChannelId(final String channelId) {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO athlete (code, youtube_channel_id) VALUES ('other', ?)", channelId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"kim_angel){id}", "kim angel", "", "a_username_longer_than_thirty_c"})
  void rejectsAnInvalidInstagramUsername(final String username) {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO athlete (code, instagram_username) VALUES ('other', ?)", username))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsAnAccountSharedByTwoAthletes() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO athlete (code, instagram_username) VALUES ('other', 'kim_angel')"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void anUnknownAthleteIsEmpty() {
    assertThat(athleteRepository.findByCode("unknown")).isEmpty();
  }

  @Test
  void returnsOnlyTheCompetitionsConfirmedByTheAthlete() {
    final long kimAngel = athleteId("kim-angel");
    final long other = insertAthlete("other-athlete");
    insertParticipation(kimAngel, "ifbb-1", true);
    insertParticipation(kimAngel, "ifbb-2", false);
    insertParticipation(other, "ifbb-3", true);

    assertThat(participationRepository.findConfirmedCompetitionIds("kim-angel"))
        .containsExactly("ifbb-1");
  }

  @Test
  void anAthleteWithoutParticipationsHasNoConfirmedCompetitions() {
    assertThat(participationRepository.findConfirmedCompetitionIds("kim-angel")).isEmpty();
  }

  @Test
  void theConfirmedCompetitionsAreCachedUntilEvicted() {
    final long kimAngel = athleteId("kim-angel");
    insertParticipation(kimAngel, "ifbb-1", true);
    assertThat(participationRepository.findConfirmedCompetitionIds("kim-angel"))
        .containsExactly("ifbb-1");

    insertParticipation(kimAngel, "ifbb-2", true);

    assertThat(participationRepository.findConfirmedCompetitionIds("kim-angel"))
        .containsExactly("ifbb-1");
    cacheManager.getCache(CacheNames.CONFIRMED_COMPETITIONS).evict("kim-angel");
    assertThat(participationRepository.findConfirmedCompetitionIds("kim-angel"))
        .containsExactlyInAnyOrder("ifbb-1", "ifbb-2");
  }

  @Test
  void anUnknownAthleteIsCachedToo() {
    assertThat(athleteRepository.findByCode("new-athlete")).isEmpty();

    insertAthlete("new-athlete");

    assertThat(athleteRepository.findByCode("new-athlete")).isEmpty();
  }

  @Test
  void rejectsAParticipationWithoutConfirmedValue() {
    final long kimAngel = athleteId("kim-angel");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO competition_participation (athlete_id, competition_id)"
                        + " VALUES (?, 'ifbb-1')",
                    kimAngel))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsACompetitionIdWithInvalidCharacters() {
    final long kimAngel = athleteId("kim-angel");

    assertThatThrownBy(() -> insertParticipation(kimAngel, "ifbb 1;", true))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsADuplicatedParticipation() {
    final long kimAngel = athleteId("kim-angel");
    insertParticipation(kimAngel, "ifbb-1", true);

    assertThatThrownBy(() -> insertParticipation(kimAngel, "ifbb-1", false))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void deletingAnAthleteDeletesItsParticipations() {
    final long other = insertAthlete("other-athlete");
    insertParticipation(other, "ifbb-1", true);

    jdbcTemplate.update("DELETE FROM athlete WHERE id = ?", other);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM competition_participation WHERE athlete_id = ?",
                Long.class,
                other))
        .isZero();
  }

  private long athleteId(final String code) {
    return jdbcTemplate.queryForObject("SELECT id FROM athlete WHERE code = ?", Long.class, code);
  }

  private long insertAthlete(final String code) {
    return jdbcTemplate.queryForObject(
        "INSERT INTO athlete (code) VALUES (?) RETURNING id", Long.class, code);
  }

  private void insertParticipation(
      final long athleteId, final String competitionId, final boolean confirmed) {
    jdbcTemplate.update(
        "INSERT INTO competition_participation (athlete_id, competition_id, confirmed)"
            + " VALUES (?, ?, ?)",
        athleteId,
        competitionId,
        confirmed);
  }
}
