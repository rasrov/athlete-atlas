package com.athlete.atlas.output.rest.youtube.client;

import com.athlete.atlas.output.rest.youtube.generated.api.YouTubeDataApi;
import com.athlete.atlas.output.rest.youtube.generated.model.ChannelListResponse;
import com.athlete.atlas.output.rest.youtube.generated.model.PlaylistItemListResponse;
import com.athlete.atlas.output.rest.youtube.generated.model.VideoListResponse;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Transport layer for the YouTube Data API. Retries are applied per request, so a transient failure
 * only repeats the failed request, and only here: neither the adapter nor the common invoker retry,
 * which would multiply the requests. Which errors are retried is configured under {@code
 * resilience4j.retry.instances.youTubeDataApi}. Spring REST client exceptions are propagated
 * untouched so that the caller can translate them.
 */
@Component
@RequiredArgsConstructor
public class YouTubeDataClient {

  public static final String BOUNDED_CONTEXT = "youtube_data_api";

  private static final String RESILIENCE_INSTANCE = "youTubeDataApi";

  private static final String CHANNEL_PARTS = "contentDetails";

  private static final String PLAYLIST_ITEM_PARTS = "contentDetails";

  private static final String VIDEO_PARTS = "snippet,status";

  private final YouTubeDataApi youTubeDataApi;

  @Retry(name = RESILIENCE_INSTANCE)
  public ChannelListResponse fetchChannel(@Nonnull final String channelId) {
    return youTubeDataApi.listChannels(CHANNEL_PARTS, channelId);
  }

  /** Fetches one page of the playlist; the first page is requested without token. */
  @Retry(name = RESILIENCE_INSTANCE)
  public PlaylistItemListResponse fetchPlaylistPage(
      @Nonnull final String playlistId, final int maxResults, @Nullable final String pageToken) {
    return youTubeDataApi.listPlaylistItems(
        PLAYLIST_ITEM_PARTS, playlistId, maxResults, pageToken);
  }

  /** Fetches the details of up to 50 videos in a single request. */
  @Retry(name = RESILIENCE_INSTANCE)
  public VideoListResponse fetchVideos(@Nonnull final List<String> videoIds) {
    return youTubeDataApi.listVideos(VIDEO_PARTS, String.join(",", videoIds));
  }
}
