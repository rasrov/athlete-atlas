package com.athlete.atlas.service;

import java.util.List;

/**
 * Provider-neutral page of a token-paginated API. {@code nextPageToken} is opaque; null or blank
 * means there are no more pages.
 */
public record TokenPage<T>(List<T> items, String nextPageToken) {}
