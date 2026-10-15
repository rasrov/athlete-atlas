package com.athlete.atlas.output.persistence.content;

import java.net.URI;
import java.time.Instant;

/**
 * Stored form of an Instagram post, decoupled from the domain record. The media type is stored as
 * text, so that a change of the domain enum is detected on read instead of corrupting the data.
 */
public record StoredInstagramPost(
    String id, String mediaType, Instant publishedAt, URI permalink, URI previewImageUrl) {}
