package com.athlete.atlas.output.rest.instagram.mapper;

import com.athlete.atlas.model.instagram.InstagramMediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parsing and validation of provider values. Each method returns empty when the value is missing or
 * invalid, so that the selection policy decides what to do with the post and the mapper only
 * transforms values already validated.
 */
public final class InstagramPostValues {

  private static final Pattern NUMERIC_ID = Pattern.compile("^[0-9]+$");

  private static final Pattern PERMALINK_PATH = Pattern.compile("^/(p|reel)/[A-Za-z0-9_-]+/$");

  private static final String INSTAGRAM_HOST = "www.instagram.com";

  private static final List<String> IMAGE_DOMAINS = List.of("cdninstagram.com", "fbcdn.net");

  /** Local date-time with a mandatory offset: Z, +00:00 or +0000 (the provider format). */
  private static final DateTimeFormatter TIMESTAMP =
      new DateTimeFormatterBuilder()
          .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
          .optionalStart()
          .appendOffset("+HH:MM", "Z")
          .optionalEnd()
          .optionalStart()
          .appendOffset("+HHMM", "Z")
          .optionalEnd()
          .toFormatter(Locale.ROOT)
          .withChronology(IsoChronology.INSTANCE)
          .withResolverStyle(ResolverStyle.STRICT);

  private InstagramPostValues() {}

  public static boolean isNumericId(final String id) {
    return id != null && NUMERIC_ID.matcher(id).matches();
  }

  /** Supported media type, or empty for any other value. */
  public static Optional<InstagramMediaType> mediaType(final String value) {
    return Arrays.stream(InstagramMediaType.values())
        .filter(type -> type.name().equals(value))
        .findFirst();
  }

  /** Publication instant, or empty if the value is invalid or has no offset. */
  public static Optional<Instant> publishedAt(final String value) {
    if (value == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(OffsetDateTime.parse(value, TIMESTAMP).toInstant());
    } catch (final DateTimeException e) {
      return Optional.empty();
    }
  }

  /**
   * Permalink in the form https://www.instagram.com/p/{code}/ or https://www.instagram.com/reel/{code}/,
   * without user info, port, query or fragment.
   */
  public static Optional<URI> permalink(final String value) {
    return uri(value)
        .filter(uri -> "https".equals(uri.getScheme()))
        .filter(uri -> INSTAGRAM_HOST.equals(uri.getHost()))
        .filter(uri -> uri.getRawUserInfo() == null && uri.getPort() == -1)
        .filter(uri -> uri.getRawQuery() == null && uri.getRawFragment() == null)
        .filter(uri -> uri.getRawPath() != null && PERMALINK_PATH.matcher(uri.getRawPath()).matches());
  }

  /**
   * Cover image of the post: thumbnail_url for videos, media_url for images and carousels. Only
   * https URLs without user info, with no port or 443, on cdninstagram.com, fbcdn.net or one of
   * their subdomains are accepted.
   */
  public static Optional<URI> previewImageUrl(
      final InstagramMediaType mediaType, final String mediaUrl, final String thumbnailUrl) {
    final var value = mediaType == InstagramMediaType.VIDEO ? thumbnailUrl : mediaUrl;
    return uri(value)
        .filter(uri -> "https".equals(uri.getScheme()))
        .filter(uri -> uri.getRawUserInfo() == null)
        .filter(uri -> uri.getPort() == -1 || uri.getPort() == 443)
        .filter(uri -> isImageHost(uri.getHost()));
  }

  /** Respects domain boundaries: evilfbcdn.net or fbcdn.net.evil.com are rejected. */
  private static boolean isImageHost(final String host) {
    if (host == null) {
      return false;
    }
    final var normalized = host.toLowerCase(Locale.ROOT);
    return IMAGE_DOMAINS.stream()
        .anyMatch(domain -> normalized.equals(domain) || normalized.endsWith("." + domain));
  }

  private static Optional<URI> uri(final String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(new URI(value));
    } catch (final URISyntaxException e) {
      return Optional.empty();
    }
  }
}
