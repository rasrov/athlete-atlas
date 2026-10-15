package com.athlete.atlas.output.persistence.content;

import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.competition.CompetitionCatalog;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.output.persistence.StorageCalls;
import com.athlete.atlas.port.output.CompetitionCatalogRepository;
import com.fasterxml.jackson.core.JacksonException;
import jakarta.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Stores the competition catalog in competition_catalog, one row per season, and serves the latest
 * season from memory. Same read and write rules as {@link ContentSelectionRepositoryAdapter}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionCatalogRepositoryAdapter implements CompetitionCatalogRepository {

  private static final String FIND_LATEST =
      """
      SELECT season_start, season_end_exclusive, items, updated_at, last_refresh_failed
      FROM competition_catalog
      ORDER BY season_start DESC
      LIMIT 1
      """;

  private static final String UPSERT =
      """
      INSERT INTO competition_catalog
          (season_start, season_end_exclusive, items, updated_at, last_refresh_failed)
      VALUES (?, ?, CAST(? AS jsonb), ?, ?)
      ON CONFLICT (season_start) DO UPDATE
      SET season_end_exclusive = EXCLUDED.season_end_exclusive,
          items = EXCLUDED.items,
          updated_at = EXCLUDED.updated_at,
          last_refresh_failed = EXCLUDED.last_refresh_failed
      """;

  private final JdbcTemplate jdbcTemplate;

  private final StoredContentMapper storedContentMapper;

  /** Null until loaded from the table; an empty Optional means that there is no stored catalog. */
  private final AtomicReference<Optional<CompetitionCatalog>> catalog = new AtomicReference<>();

  /**
   * @throws StorageUnavailableException if the catalog is not in memory yet and the database does
   *     not respond.
   */
  @Override
  public Optional<CompetitionCatalog> find() {
    final var cached = catalog.get();
    if (cached != null) {
      return cached;
    }
    final var loaded = load();
    // A refresh saved meanwhile is newer than what was loaded, so it is kept.
    return catalog.compareAndSet(null, loaded) ? loaded : catalog.get();
  }

  @Override
  public void save(@Nonnull final CompetitionCatalog competitionCatalog) {
    catalog.set(Optional.of(competitionCatalog));
    final var season = competitionCatalog.season();
    final var competitions = competitionCatalog.competitions();
    final var items =
        ContentJson.write(
            competitions.items().stream().map(storedContentMapper::toStored).toList());
    try {
      StorageCalls.call(
          () ->
              jdbcTemplate.update(
                  UPSERT,
                  season.start(),
                  season.endExclusive(),
                  items,
                  OffsetDateTime.ofInstant(competitions.updatedAt(), ZoneOffset.UTC),
                  competitions.lastRefreshFailed()));
    } catch (final StorageUnavailableException e) {
      log.warn(
          "Competitions of season {} not stored, they are only kept in memory: {}",
          season.start(),
          e.getMessage());
    }
  }

  private Optional<CompetitionCatalog> load() {
    return StorageCalls.call(
        () ->
            jdbcTemplate.query(FIND_LATEST, (rs, rowNum) -> read(rs)).stream()
                .findFirst()
                .flatMap(stored -> stored));
  }

  /** A row that cannot be read is logged and ignored: the next refresh replaces it. */
  private Optional<CompetitionCatalog> read(final ResultSet rs) throws SQLException {
    final var seasonStart = rs.getObject("season_start", LocalDate.class);
    try {
      final var competitions =
          ContentJson.readList(rs.getString("items"), StoredCompetition.class).stream()
              .map(storedContentMapper::toDomain)
              .toList();
      return Optional.of(
          new CompetitionCatalog(
              new DateRange(seasonStart, rs.getObject("season_end_exclusive", LocalDate.class)),
              new SynchronizedSelection<>(
                  competitions,
                  rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
                  rs.getBoolean("last_refresh_failed"))));
    } catch (final JacksonException | IllegalArgumentException e) {
      log.warn(
          "Stored competitions of season {} cannot be read and are ignored: {}",
          seasonStart,
          e.getMessage());
      return Optional.empty();
    }
  }
}
