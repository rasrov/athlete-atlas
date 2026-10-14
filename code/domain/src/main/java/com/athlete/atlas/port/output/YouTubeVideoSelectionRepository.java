package com.athlete.atlas.port.output;

import com.athlete.atlas.model.youtube.YouTubeVideoSelection;
import jakarta.annotation.Nonnull;
import java.util.Optional;

/** Stores the last video selection of each athlete. */
public interface YouTubeVideoSelectionRepository {

  Optional<YouTubeVideoSelection> findByAthleteId(@Nonnull String athleteId);

  void save(@Nonnull String athleteId, @Nonnull YouTubeVideoSelection selection);
}
