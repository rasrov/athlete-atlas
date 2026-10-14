package com.athlete.atlas.model.youtube;

import com.athlete.atlas.model.enums.Availability;
import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.List;

/** Videos served for an athlete, with the freshness metadata of their selection. */
public record AthleteYouTubeVideos(
    @Nonnull String athleteId,
    @Nonnull List<YouTubeVideo> videos,
    @Nonnull Instant updatedAt,
    @Nonnull Availability availability) {}
