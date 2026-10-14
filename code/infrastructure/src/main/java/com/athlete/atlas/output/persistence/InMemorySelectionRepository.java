package com.athlete.atlas.output.persistence;

import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.port.output.SelectionRepository;
import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage until the backend has a database: selections are lost on restart and rebuilt
 * by the refresh on startup. Each instance keeps its own selections. Saving replaces the whole
 * selection atomically.
 */
public abstract class InMemorySelectionRepository<T> implements SelectionRepository<T> {

  private final Map<String, SynchronizedSelection<T>> selections = new ConcurrentHashMap<>();

  @Override
  public Optional<SynchronizedSelection<T>> findByAthleteId(@Nonnull final String athleteId) {
    return Optional.ofNullable(selections.get(athleteId));
  }

  @Override
  public void save(
      @Nonnull final String athleteId, @Nonnull final SynchronizedSelection<T> selection) {
    selections.put(athleteId, selection);
  }
}
