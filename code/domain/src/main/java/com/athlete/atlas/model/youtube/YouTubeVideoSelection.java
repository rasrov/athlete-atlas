package com.athlete.atlas.model.youtube;

import com.athlete.atlas.model.enums.Availability;
import jakarta.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Videos obtained by the last successful refresh of an athlete. It may hold fewer videos than the
 * selection size (even none): that is valid data, not an error.
 *
 * @param updatedAt when the videos were obtained.
 * @param lastRefreshFailed whether a later refresh failed, so the videos may be outdated.
 */
public record YouTubeVideoSelection(
    @Nonnull List<YouTubeVideo> videos, @Nonnull Instant updatedAt, boolean lastRefreshFailed) {

  public YouTubeVideoSelection {
    videos = List.copyOf(videos);
  }

  public static YouTubeVideoSelection refreshedAt(
      @Nonnull final List<YouTubeVideo> videos, @Nonnull final Instant updatedAt) {
    return new YouTubeVideoSelection(videos, updatedAt, false);
  }

  /** Keeps the videos and their acquisition time; only records that the refresh failed. */
  public YouTubeVideoSelection withFailedRefresh() {
    return new YouTubeVideoSelection(videos, updatedAt, true);
  }

  public Availability availability(
      @Nonnull final Instant now, @Nonnull final Duration freshnessThreshold) {
    return lastRefreshFailed || updatedAt.plus(freshnessThreshold).isBefore(now)
        ? Availability.STALE
        : Availability.AVAILABLE;
  }
}
