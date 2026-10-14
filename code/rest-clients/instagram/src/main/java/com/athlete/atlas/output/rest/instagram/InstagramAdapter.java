package com.athlete.atlas.output.rest.instagram;

import static com.athlete.atlas.exception.RestClientErrorMapper.mapRemoteException;

import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.output.rest.instagram.client.InstagramGraphClient;
import com.athlete.atlas.output.rest.instagram.config.InstagramClientProperties;
import com.athlete.atlas.output.rest.instagram.generated.model.BusinessDiscovery;
import com.athlete.atlas.output.rest.instagram.generated.model.GraphError;
import com.athlete.atlas.output.rest.instagram.generated.model.GraphErrorResponse;
import com.athlete.atlas.output.rest.instagram.generated.model.Media;
import com.athlete.atlas.output.rest.instagram.generated.model.MediaPage;
import com.athlete.atlas.output.rest.instagram.mapper.InstagramPostMapper;
import com.athlete.atlas.output.rest.instagram.mapper.InstagramPostValues;
import com.athlete.atlas.port.output.InstagramPort;
import com.athlete.atlas.service.InvokerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Selects the latest posts of an Instagram profile with a single Business Discovery request (no
 * pagination). Posts with an unsupported type or a future date are skipped; posts with invalid data
 * (non numeric id, invalid permalink or timestamp) are discarded and logged. Fewer posts than the
 * selection size is a valid result.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramAdapter implements InstagramPort {

  /** Instagram usernames: letters, digits, dots and underscores, up to 30 characters. */
  public static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._]{1,30}$");

  private static final String FIELDS =
      "business_discovery.username(%s){username,media.limit(%d)"
          + "{id,permalink,media_type,media_url,thumbnail_url,timestamp}}";

  /** Latest first, then numeric id ascending, as the public contract defines. */
  private static final Comparator<InstagramPost> LATEST_FIRST =
      Comparator.comparing(InstagramPost::publishedAt)
          .reversed()
          .thenComparing(post -> new BigInteger(post.id()));

  private static final int INVALID_TOKEN_CODE = 190;

  private static final int PERMISSION_CODE = 10;

  private final InstagramGraphClient instagramGraphClient;

  private final InvokerService invokerService;

  private final InstagramPostMapper instagramPostMapper;

  private final InstagramClientProperties instagramClientProperties;

  private final ObjectMapper objectMapper;

  private final Clock clock;

  @Override
  public List<InstagramPost> fetchLatestPosts(@Nonnull final String username) {
    if (!USERNAME.matcher(username).matches()) {
      throw new IllegalArgumentException("Invalid Instagram username");
    }
    final var fields = FIELDS.formatted(username, instagramClientProperties.mediaLimit());
    final var response =
        invokerService.invoke(
            () -> instagramGraphClient.fetchBusinessDiscovery(fields), this::mapError);

    if (response == null) {
      throw remoteError("empty response");
    }
    if (response.getError() != null) {
      logGraphError(response.getError());
      throw remoteError("provider error");
    }
    final var discovery = response.getBusinessDiscovery();
    if (discovery == null || !username.equalsIgnoreCase(discovery.getUsername())) {
      log.warn(
          "[{}] Business Discovery did not return the requested profile {}",
          InstagramGraphClient.BOUNDED_CONTEXT,
          username);
      throw remoteError("unexpected profile");
    }

    return select(mediaOf(discovery));
  }

  private List<InstagramPost> select(final List<Media> media) {
    final var now = clock.instant();
    final List<String> invalidIds = new ArrayList<>();
    final var byId = new LinkedHashMap<String, InstagramPost>();
    for (final Media item : media) {
      toPost(item, now, invalidIds).ifPresent(post -> byId.putIfAbsent(post.id(), post));
    }
    if (!invalidIds.isEmpty()) {
      log.warn(
          "[{}] Discarded {} of {} posts with invalid data. Ids: {}",
          InstagramGraphClient.BOUNDED_CONTEXT,
          invalidIds.size(),
          media.size(),
          invalidIds);
    }
    return byId.values().stream()
        .sorted(LATEST_FIRST)
        .limit(instagramClientProperties.selectionSize())
        .toList();
  }

  /** Selection policy: decides whether a post is kept before mapping it. */
  private Optional<InstagramPost> toPost(
      final Media media, final Instant now, final List<String> invalidIds) {
    if (InstagramPostValues.mediaType(media.getMediaType()).isEmpty()) {
      return Optional.empty();
    }
    final var publishedAt = InstagramPostValues.publishedAt(media.getTimestamp());
    if (!InstagramPostValues.isNumericId(media.getId())
        || InstagramPostValues.permalink(media.getPermalink()).isEmpty()
        || publishedAt.isEmpty()) {
      invalidIds.add(media.getId());
      return Optional.empty();
    }
    if (publishedAt.get().isAfter(now)) {
      return Optional.empty();
    }
    return Optional.of(instagramPostMapper.toDomain(media));
  }

  private static List<Media> mediaOf(final BusinessDiscovery discovery) {
    return Optional.ofNullable(discovery.getMedia())
        .map(MediaPage::getData)
        .orElse(List.of())
        .stream()
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Translates REST client errors. Rejected credentials and missing permissions become an
   * identifiable error; only the code, type and subcode of the provider error are logged.
   */
  private RemoteException mapError(final RuntimeException e) {
    final var mapped = mapRemoteException(e, InstagramGraphClient.BOUNDED_CONTEXT);
    if (!(e instanceof final HttpStatusCodeException statusEx)) {
      return mapped;
    }
    final var error = parseGraphError(statusEx);
    error.ifPresent(this::logGraphError);
    final var code = error.map(GraphError::getCode).orElse(null);
    if (Integer.valueOf(INVALID_TOKEN_CODE).equals(code)) {
      return new RemoteException(
          remoteMessage("access token invalid or expired"), mapped.getStatusCode(), e);
    }
    if (isPermissionError(code)) {
      return new RemoteException(
          remoteMessage("missing permissions"), mapped.getStatusCode(), e);
    }
    return mapped;
  }

  private Optional<GraphError> parseGraphError(final HttpStatusCodeException e) {
    try {
      return Optional.ofNullable(
              objectMapper.readValue(e.getResponseBodyAsByteArray(), GraphErrorResponse.class))
          .map(GraphErrorResponse::getError);
    } catch (final IOException | RuntimeException parseError) {
      return Optional.empty();
    }
  }

  private void logGraphError(final GraphError error) {
    final var code = error.getCode();
    if (Integer.valueOf(INVALID_TOKEN_CODE).equals(code)) {
      log.error(
          "[{}] Access token rejected (code {}, subcode {}): renew INSTAGRAM_ACCESS_TOKEN",
          InstagramGraphClient.BOUNDED_CONTEXT,
          code,
          error.getErrorSubcode());
    } else if (isPermissionError(code)) {
      log.error(
          "[{}] Missing permissions (code {}, subcode {}): review the token permissions",
          InstagramGraphClient.BOUNDED_CONTEXT,
          code,
          error.getErrorSubcode());
    } else {
      log.warn(
          "[{}] Provider error (code {}, type {}, subcode {})",
          InstagramGraphClient.BOUNDED_CONTEXT,
          code,
          error.getType(),
          error.getErrorSubcode());
    }
  }

  /** Graph API permission errors: code 10 and the 200-299 range. */
  private static boolean isPermissionError(final Integer code) {
    return code != null && (code == PERMISSION_CODE || (code >= 200 && code <= 299));
  }

  private static RemoteException remoteError(final String reason) {
    return new RemoteException(remoteMessage(reason), HttpStatus.BAD_GATEWAY.value(), null);
  }

  private static String remoteMessage(final String reason) {
    return String.format("[%s] %s", InstagramGraphClient.BOUNDED_CONTEXT, reason);
  }
}
