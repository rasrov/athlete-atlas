package com.athlete.atlas.model.instagram;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URI;
import java.time.Instant;

/**
 * Instagram post linked from the website.
 *
 * @param id numeric Instagram id, kept as a string to avoid precision loss.
 * @param previewImageUrl temporary cover image (video thumbnail or image/carousel cover); null when
 *     the provider gives no valid URL. The post is still shown through its permalink.
 */
public record InstagramPost(
    @Nonnull String id,
    @Nonnull InstagramMediaType mediaType,
    @Nonnull Instant publishedAt,
    @Nonnull URI permalink,
    @Nullable URI previewImageUrl) {}
