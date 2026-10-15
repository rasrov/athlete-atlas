package com.athlete.atlas.model.athlete;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Athlete managed by the backend.
 *
 * @param code public identifier used in the API (e.g. kim-angel).
 * @param youTubeChannelId public id of the YouTube channel of the athlete; null if none.
 * @param instagramUsername public username of the Instagram profile of the athlete; null if none.
 */
public record Athlete(
    @Nonnull String code,
    @Nullable String youTubeChannelId,
    @Nullable String instagramUsername) {}
