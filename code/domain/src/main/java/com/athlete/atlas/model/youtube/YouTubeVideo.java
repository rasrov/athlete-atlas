package com.athlete.atlas.model.youtube;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URI;
import java.time.Instant;

public record YouTubeVideo(
    @Nonnull String id,
    @Nonnull String title,
    @Nonnull Instant publishedAt,
    @Nonnull URI watchUrl,
    @Nonnull URI embedUrl,
    @Nullable URI thumbnailUrl) {}
