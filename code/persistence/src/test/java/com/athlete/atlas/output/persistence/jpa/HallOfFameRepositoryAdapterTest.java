package com.athlete.atlas.output.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.athlete.atlas.model.halloffame.HallOfFameEntry;
import com.athlete.atlas.model.halloffame.HallOfFameImage;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

/** Hall of fame tables and adapter on a real PostgreSQL, with the real cache provider. */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureCache(cacheProvider = CacheType.CAFFEINE)
@Import({HallOfFameRepositoryAdapter.class, HallOfFameRepositoryAdapterTest.CachingConfiguration.class})
@TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.cache.cache-names=hallOfFame"
    })
class HallOfFameRepositoryAdapterTest {

  private static final String ATHLETE = "kim-angel";

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired private HallOfFameRepositoryAdapter hallOfFameRepository;

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
  void returnsTheResultsByDateDescendingThenCreationOrderWithTheirImagesInOrder() {
    final long older = insertEntry(ATHLETE, "2025-11-08", "Championship", "Classic Physique");
    final long sameDayFirst = insertEntry(ATHLETE, "2026-09-12", "Pro Show", "Classic Physique");
    final long sameDaySecond = insertEntry(ATHLETE, "2026-09-12", "Pro Show", "Open");
    insertImage(sameDayFirst, 1, "https://img.example.com/b.webp", null);
    insertImage(sameDayFirst, 0, "https://img.example.com/a.webp", "On stage");

    final var results = hallOfFameRepository.findByAthleteCode(ATHLETE);

    assertThat(results)
        .extracting(HallOfFameEntry::id)
        .containsExactly(
            String.valueOf(sameDayFirst), String.valueOf(sameDaySecond), String.valueOf(older));
    assertThat(results.get(0).images())
        .containsExactly(
            new HallOfFameImage(URI.create("https://img.example.com/a.webp"), "On stage"),
            new HallOfFameImage(URI.create("https://img.example.com/b.webp"), null));
    assertThat(results.get(1).images()).isEmpty();
  }

  @Test
  void mapsEveryField() {
    final long id =
        jdbcTemplate.queryForObject(
            "INSERT INTO hall_of_fame_entry (athlete_id, competition_date, category,"
                + " competition_name, placement, official_url, competition_id)"
                + " SELECT id, '2026-09-12', 'Classic Physique', 'Pro Show', '16.º Top mundial',"
                + " 'https://www.ifbbpro.com/competition/pro-show/', 'ifbb-12345'"
                + " FROM athlete WHERE code = ? RETURNING id",
            Long.class,
            ATHLETE);

    final var entry = hallOfFameRepository.findByAthleteCode(ATHLETE).get(0);

    assertThat(entry.id()).isEqualTo(String.valueOf(id));
    assertThat(entry.date()).isEqualTo(LocalDate.of(2026, 9, 12));
    assertThat(entry.category()).isEqualTo("Classic Physique");
    assertThat(entry.competitionName()).isEqualTo("Pro Show");
    assertThat(entry.placement()).isEqualTo("16.º Top mundial");
    assertThat(entry.officialUrl())
        .isEqualTo(URI.create("https://www.ifbbpro.com/competition/pro-show/"));
    assertThat(entry.competitionId()).isEqualTo("ifbb-12345");
    assertThat(entry.updatedAt()).isNotNull();
  }

  @Test
  void returnsOnlyTheResultsOfTheAthlete() {
    insertAthlete("other-athlete");
    insertEntry("other-athlete", "2026-09-12", "Pro Show", "Classic Physique");

    assertThat(hallOfFameRepository.findByAthleteCode(ATHLETE)).isEmpty();
    assertThat(hallOfFameRepository.findByAthleteCode("unknown")).isEmpty();
  }

  @Test
  void theResultsAreCachedUntilEvicted() {
    insertEntry(ATHLETE, "2025-11-08", "Championship", "Classic Physique");
    assertThat(hallOfFameRepository.findByAthleteCode(ATHLETE)).hasSize(1);

    insertEntry(ATHLETE, "2026-09-12", "Pro Show", "Classic Physique");

    assertThat(hallOfFameRepository.findByAthleteCode(ATHLETE)).hasSize(1);
    cacheManager.getCache(CacheNames.HALL_OF_FAME).evict(ATHLETE);
    assertThat(hallOfFameRepository.findByAthleteCode(ATHLETE)).hasSize(2);
  }

  @Test
  void rejectsADuplicatedResult() {
    insertEntry(ATHLETE, "2026-09-12", "Pro Show", "Classic Physique");

    assertThatThrownBy(() -> insertEntry(ATHLETE, "2026-09-12", "Pro Show", "Classic Physique"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "competition_date = '1949-12-31'",
        "category = '  '",
        "competition_name = ''",
        "placement = ' '",
        "official_url = 'http://insecure.example.com'",
        "competition_id = 'ifbb 1;'"
      })
  void rejectsAnInvalidResult(final String invalidColumn) {
    final long id = insertEntry(ATHLETE, "2026-09-12", "Pro Show", "Classic Physique");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE hall_of_fame_entry SET " + invalidColumn + " WHERE id = ?", id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  /** Each case in its own test: after a failed statement PostgreSQL aborts the transaction. */
  @ParameterizedTest
  @CsvSource({
    "1, http://img.example.com/b.webp", // not https
    "0, https://img.example.com/c.webp", // repeated position
    "-1, https://img.example.com/d.webp" // negative position
  })
  void rejectsAnInvalidImage(final int position, final String url) {
    final long entry = insertEntry(ATHLETE, "2026-09-12", "Pro Show", "Classic Physique");
    insertImage(entry, 0, "https://img.example.com/a.webp", null);

    assertThatThrownBy(() -> insertImage(entry, position, url, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void deletingTheAthleteDeletesItsResultsAndImages() {
    final long entry = insertEntry(ATHLETE, "2026-09-12", "Pro Show", "Classic Physique");
    insertImage(entry, 0, "https://img.example.com/a.webp", null);

    jdbcTemplate.update("DELETE FROM athlete WHERE code = ?", ATHLETE);

    assertThat(count("hall_of_fame_entry")).isZero();
    assertThat(count("hall_of_fame_image")).isZero();
  }

  private void insertAthlete(final String code) {
    jdbcTemplate.update("INSERT INTO athlete (code) VALUES (?)", code);
  }

  private long insertEntry(
      final String athlete, final String date, final String competition, final String category) {
    return jdbcTemplate.queryForObject(
        "INSERT INTO hall_of_fame_entry"
            + " (athlete_id, competition_date, category, competition_name, placement)"
            + " SELECT id, CAST(? AS date), ?, ?, '5.º Puesto' FROM athlete WHERE code = ?"
            + " RETURNING id",
        Long.class,
        date,
        category,
        competition,
        athlete);
  }

  private void insertImage(
      final long entryId, final int position, final String url, final String altText) {
    jdbcTemplate.update(
        "INSERT INTO hall_of_fame_image (entry_id, position, url, alt_text) VALUES (?, ?, ?, ?)",
        entryId,
        position,
        url,
        altText);
  }

  private long count(final String table) {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }
}
