package com.athlete.atlas.model.selection;

import com.athlete.atlas.model.enums.Availability;
import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.List;

/** Selection served for an athlete, with its freshness metadata. */
public record AthleteSelection<T>(
    @Nonnull String athleteId,
    @Nonnull List<T> items,
    @Nonnull Instant updatedAt,
    @Nonnull Availability availability) {}
