package com.athlete.atlas.output.persistence.content;

import java.net.URI;
import java.time.Instant;

/** Stored form of a YouTube video, decoupled from the domain record. */
public record StoredYouTubeVideo(
    String id,
    String title,
    Instant publishedAt,
    URI watchUrl,
    URI embedUrl,
    URI thumbnailUrl) {}
