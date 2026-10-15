package com.athlete.atlas.output.persistence.content;

import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.output.persistence.StorageCalls;
import com.athlete.atlas.port.output.SelectionRepository;
import com.fasterxml.jackson.core.JacksonException;
import jakarta.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Stores the last selection of each athlete in content_selection and serves it from memory.
 *
 * <ul>
 *   <li>Read: from memory; on the first read of an athlete, it is loaded from the table and kept
 *       without expiry, so requests do not reach the database. Valid with a single instance.
 *   <li>Write: memory first and then the table, so that fresh data is served even if the database
 *       does not respond; the next refresh writes it again.
 * </ul>
 *
 * @param <T> domain type of the items.
 * @param <S> stored type of the items.
 */
@Slf4j
public abstract class ContentSelectionRepositoryAdapter<T, S> implements SelectionRepository<T> {

  private static final String FIND =
      """
      SELECT s.items, s.updated_at, s.last_refresh_failed
      FROM content_selection s
      JOIN athlete a ON a.id = s.athlete_id
      WHERE a.code = ? AND s.source = ?
      """;

  /** An unknown athlete inserts nothing, instead of failing on the foreign key. */
  private static final String UPSERT =
      """
      INSERT INTO content_selection (athlete_id, source, items, updated_at, last_refresh_failed)
      SELECT a.id, ?, CAST(? AS jsonb), ?, ?
      FROM athlete a
      WHERE a.code = ?
      ON CONFLICT (athlete_id, source) DO UPDATE
      SET items = EXCLUDED.items,
          updated_at = EXCLUDED.updated_at,
          last_refresh_failed = EXCLUDED.last_refresh_failed
      """;

  private final Map<String, Optional<SynchronizedSelection<T>>> selections =
      new ConcurrentHashMap<>();

  private final JdbcTemplate jdbcTemplate;

  private final String source;

  private final Class<S> storedType;

  /**
   * @param source value of the source column (e.g. youtube).
   */
  protected ContentSelectionRepositoryAdapter(
      final JdbcTemplate jdbcTemplate, final String source, final Class<S> storedType) {
    this.jdbcTemplate = jdbcTemplate;
    this.source = source;
    this.storedType = storedType;
  }

  protected abstract S toStored(T item);

  protected abstract T toDomain(S stored);

  /**
   * @throws StorageUnavailableException if the selection is not in memory yet and the database does
   *     not respond; nothing is kept in memory, so the next read tries again.
   */
  @Override
  public Optional<SynchronizedSelection<T>> findByAthleteId(@Nonnull final String athleteId) {
    final var cached = selections.get(athleteId);
    if (cached != null) {
      return cached;
    }
    final var loaded = load(athleteId);
    // A refresh saved meanwhile is newer than what was loaded, so it is kept.
    final var previous = selections.putIfAbsent(athleteId, loaded);
    return previous != null ? previous : loaded;
  }

  @Override
  public void save(
      @Nonnull final String athleteId, @Nonnull final SynchronizedSelection<T> selection) {
    selections.put(athleteId, Optional.of(selection));
    final var items = ContentJson.write(selection.items().stream().map(this::toStored).toList());
    try {
      StorageCalls.call(
          () ->
              jdbcTemplate.update(
                  UPSERT,
                  source,
                  items,
                  OffsetDateTime.ofInstant(selection.updatedAt(), ZoneOffset.UTC),
                  selection.lastRefreshFailed(),
                  athleteId));
    } catch (final StorageUnavailableException e) {
      log.warn(
          "{} selection of athlete {} not stored, it is only kept in memory: {}",
          source,
          athleteId,
          e.getMessage());
    }
  }

  private Optional<SynchronizedSelection<T>> load(final String athleteId) {
    return StorageCalls.call(
        () ->
            jdbcTemplate
                .query(FIND, (rs, rowNum) -> read(athleteId, rs), athleteId, source)
                .stream()
                .findFirst()
                .flatMap(selection -> selection));
  }

  /** A row that cannot be read is logged and ignored: the next refresh replaces it. */
  private Optional<SynchronizedSelection<T>> read(final String athleteId, final ResultSet rs)
      throws SQLException {
    try {
      final var items =
          ContentJson.readList(rs.getString("items"), storedType).stream()
              .map(this::toDomain)
              .toList();
      return Optional.of(
          new SynchronizedSelection<>(
              items,
              rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
              rs.getBoolean("last_refresh_failed")));
    } catch (final JacksonException | IllegalArgumentException e) {
      log.warn(
          "Stored {} selection of athlete {} cannot be read and is ignored: {}",
          source,
          athleteId,
          e.getMessage());
      return Optional.empty();
    }
  }
}
