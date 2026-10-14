package com.athlete.atlas.model.selection;

import com.athlete.atlas.model.enums.Availability;
import jakarta.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Items obtained by the last successful refresh of an athlete (e.g. YouTube videos, Instagram
 * posts). It may hold fewer items than the selection size, even none: that is valid data, not an
 * error.
 *
 * @param updatedAt when the items were obtained; a failed refresh does not advance it.
 * @param lastRefreshFailed whether a later refresh failed, so the items may be outdated.
 */
public record SynchronizedSelection<T>(
    @Nonnull List<T> items, @Nonnull Instant updatedAt, boolean lastRefreshFailed) {

  public SynchronizedSelection {
    items = List.copyOf(items);
  }

  public static <T> SynchronizedSelection<T> refreshedAt(
      @Nonnull final List<T> items, @Nonnull final Instant updatedAt) {
    return new SynchronizedSelection<>(items, updatedAt, false);
  }

  /** Keeps the items and their acquisition time; only records that the refresh failed. */
  public SynchronizedSelection<T> withFailedRefresh() {
    return new SynchronizedSelection<>(items, updatedAt, true);
  }

  public Availability availability(
      @Nonnull final Instant now, @Nonnull final Duration freshnessThreshold) {
    return lastRefreshFailed || updatedAt.plus(freshnessThreshold).isBefore(now)
        ? Availability.STALE
        : Availability.AVAILABLE;
  }
}
