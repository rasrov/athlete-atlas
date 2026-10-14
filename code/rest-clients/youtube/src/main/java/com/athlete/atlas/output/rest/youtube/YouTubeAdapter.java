package com.athlete.atlas.output.rest.youtube;

import static com.athlete.atlas.exception.RestClientErrorMapper.mapRemoteException;

import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.output.rest.youtube.client.YouTubeDataClient;
import com.athlete.atlas.output.rest.youtube.config.YouTubeClientProperties;
import com.athlete.atlas.output.rest.youtube.generated.model.Channel;
import com.athlete.atlas.output.rest.youtube.generated.model.ChannelContentDetails;
import com.athlete.atlas.output.rest.youtube.generated.model.ChannelListResponse;
import com.athlete.atlas.output.rest.youtube.generated.model.PlaylistItem;
import com.athlete.atlas.output.rest.youtube.generated.model.PlaylistItemContentDetails;
import com.athlete.atlas.output.rest.youtube.generated.model.RelatedPlaylists;
import com.athlete.atlas.output.rest.youtube.generated.model.Video;
import com.athlete.atlas.output.rest.youtube.generated.model.VideoListResponse;
import com.athlete.atlas.output.rest.youtube.mapper.YouTubeVideoMapper;
import com.athlete.atlas.port.output.YouTubePort;
import com.athlete.atlas.service.InvokerService;
import com.athlete.atlas.service.TokenPage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Selects the latest videos of a channel: reads its uploads playlist page by page, fetches the
 * details of each page and keeps the eligible videos until the selection is complete, the playlist
 * ends or the page limit is reached. Fewer videos than the selection size is a valid result.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeAdapter implements YouTubePort {

  private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

  private static final Set<String> EXCLUDED_BROADCASTS = Set.of("live", "upcoming");

  private static final Comparator<YouTubeVideo> LATEST_FIRST =
      Comparator.comparing(YouTubeVideo::publishedAt)
          .reversed()
          .thenComparing(YouTubeVideo::id);

  private final YouTubeDataClient youTubeDataClient;

  private final InvokerService invokerService;

  private final YouTubeVideoMapper youTubeVideoMapper;

  private final YouTubeClientProperties youTubeClientProperties;

  private final Clock clock;

  @Override
  public List<YouTubeVideo> fetchLatestVideos(@Nonnull final String channelId) {
    final var uploadsPlaylistId = fetchUploadsPlaylistId(channelId);
    final var now = clock.instant();
    final var selectionSize = youTubeClientProperties.selectionSize();
    // Shared across pages: an id already requested is not requested again.
    final Set<String> requestedIds = new HashSet<>();

    final var videos =
        invokerService.invokeTokenPaginated(
            pageToken ->
                fetchEligibleVideos(channelId, uploadsPlaylistId, pageToken, requestedIds, now),
            found -> distinctById(found).size() >= selectionSize,
            youTubeClientProperties.maxPages(),
            YouTubeAdapter::mapError);

    return distinctById(videos).stream().sorted(LATEST_FIRST).limit(selectionSize).toList();
  }

  private String fetchUploadsPlaylistId(final String channelId) {
    final var response =
        invokerService.invoke(
            () -> youTubeDataClient.fetchChannel(channelId), YouTubeAdapter::mapError);
    return Optional.ofNullable(response)
        .map(ChannelListResponse::getItems)
        .flatMap(items -> items.stream().filter(Objects::nonNull).findFirst())
        .map(Channel::getContentDetails)
        .map(ChannelContentDetails::getRelatedPlaylists)
        .map(RelatedPlaylists::getUploads)
        .filter(uploads -> !uploads.isBlank())
        .orElseThrow(
            () -> {
              log.warn(
                  "[{}] Channel {} not found or without uploads playlist",
                  YouTubeDataClient.BOUNDED_CONTEXT,
                  channelId);
              return new RemoteException(
                  String.format(
                      "[%s] channel without uploads playlist", YouTubeDataClient.BOUNDED_CONTEXT),
                  HttpStatus.BAD_GATEWAY.value(),
                  null);
            });
  }

  /** One playlist page with the details of its new ids, already filtered. */
  private TokenPage<YouTubeVideo> fetchEligibleVideos(
      final String channelId,
      final String playlistId,
      @Nullable final String pageToken,
      final Set<String> requestedIds,
      final Instant now) {
    final var page =
        youTubeDataClient.fetchPlaylistPage(
            playlistId, youTubeClientProperties.pageSize(), pageToken);
    if (page == null) {
      return null;
    }
    final List<String> newIds =
        Optional.ofNullable(page.getItems()).orElse(List.of()).stream()
            .filter(Objects::nonNull)
            .map(PlaylistItem::getContentDetails)
            .filter(Objects::nonNull)
            .map(PlaylistItemContentDetails::getVideoId)
            .filter(id -> id != null && requestedIds.add(id))
            .toList();

    final List<Video> details =
        newIds.isEmpty()
            ? List.of()
            : Optional.ofNullable(youTubeDataClient.fetchVideos(newIds))
                .map(VideoListResponse::getItems)
                .orElse(List.of());
    final var eligible =
        details.stream()
            .filter(video -> isEligible(video, channelId, now))
            .map(youTubeVideoMapper::toDomain)
            .toList();

    log.debug(
        "[{}] Page with {} new ids: {} details, {} eligible",
        YouTubeDataClient.BOUNDED_CONTEXT,
        newIds.size(),
        details.size(),
        eligible.size());
    return new TokenPage<>(eligible, page.getNextPageToken());
  }

  /**
   * Public, embeddable and already published videos of the requested channel, with a valid id and
   * a title. Live and upcoming broadcasts are excluded; Shorts are not.
   */
  private static boolean isEligible(final Video video, final String channelId, final Instant now) {
    if (video == null || video.getId() == null || !VIDEO_ID.matcher(video.getId()).matches()) {
      return false;
    }
    final var snippet = video.getSnippet();
    final var status = video.getStatus();
    return snippet != null
        && status != null
        && channelId.equals(snippet.getChannelId())
        && snippet.getTitle() != null
        && !snippet.getTitle().isBlank()
        && snippet.getPublishedAt() != null
        && !snippet.getPublishedAt().toInstant().isAfter(now)
        && !EXCLUDED_BROADCASTS.contains(snippet.getLiveBroadcastContent())
        && "public".equals(status.getPrivacyStatus())
        && Boolean.TRUE.equals(status.getEmbeddable());
  }

  /** Keeps the first occurrence of each id. */
  private static Collection<YouTubeVideo> distinctById(final List<YouTubeVideo> videos) {
    final var byId = new LinkedHashMap<String, YouTubeVideo>();
    videos.forEach(video -> byId.putIfAbsent(video.id(), video));
    return byId.values();
  }

  private static RemoteException mapError(final RuntimeException e) {
    return mapRemoteException(e, YouTubeDataClient.BOUNDED_CONTEXT);
  }
}
