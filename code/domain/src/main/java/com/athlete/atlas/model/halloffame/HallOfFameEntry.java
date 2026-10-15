package com.athlete.atlas.model.halloffame;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Competition result of an athlete, managed editorially.
 *
 * @param id opaque identifier.
 * @param date date of the competition (first day if it lasts several days).
 * @param placement result as displayed (e.g. 5.º Puesto), without further formatting.
 * @param competitionId calendar competition of this result, when known.
 * @param images images in editorial order.
 * @param updatedAt last modification of the result.
 */
public record HallOfFameEntry(
    @Nonnull String id,
    @Nonnull LocalDate date,
    @Nonnull String category,
    @Nonnull String competitionName,
    @Nonnull String placement,
    @Nullable URI officialUrl,
    @Nullable String competitionId,
    @Nonnull List<HallOfFameImage> images,
    @Nonnull Instant updatedAt) {

  public HallOfFameEntry {
    images = List.copyOf(images);
  }
}
