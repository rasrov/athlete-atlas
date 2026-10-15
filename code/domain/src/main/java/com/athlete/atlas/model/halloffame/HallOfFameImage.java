package com.athlete.atlas.model.halloffame;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URI;

/**
 * Image of a competition result.
 *
 * @param altText alternative text for accessibility.
 */
public record HallOfFameImage(@Nonnull URI url, @Nullable String altText) {}
