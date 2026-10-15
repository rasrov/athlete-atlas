package com.athlete.atlas.model.selection;

import com.athlete.atlas.model.enums.Availability;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

/**
 * Selection served for an athlete, with its freshness metadata.
 *
 * @param updatedAt when the items were obtained; null when it is unknown (availability UNKNOWN).
 */
public record AthleteSelection<T>(
    @Nonnull String athleteId,
    @Nonnull List<T> items,
    @Nullable Instant updatedAt,
    @Nonnull Availability availability) {}
