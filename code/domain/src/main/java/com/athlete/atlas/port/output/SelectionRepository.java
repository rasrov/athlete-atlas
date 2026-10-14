package com.athlete.atlas.port.output;

import com.athlete.atlas.model.selection.SynchronizedSelection;
import jakarta.annotation.Nonnull;
import java.util.Optional;

/**
 * Stores the last selection of each athlete. Each kind of content has its own sub-interface (e.g.
 * {@link YouTubeVideoSelectionRepository}) so that implementations can be injected by type.
 */
public interface SelectionRepository<T> {

  Optional<SynchronizedSelection<T>> findByAthleteId(@Nonnull String athleteId);

  /** Replaces the whole selection of the athlete. */
  void save(@Nonnull String athleteId, @Nonnull SynchronizedSelection<T> selection);
}
