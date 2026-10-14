package com.athlete.atlas.port.output;

import com.athlete.atlas.model.youtube.YouTubeVideo;
import jakarta.annotation.Nonnull;
import java.util.List;

public interface YouTubePort {

  /**
   * Fetches the latest public and embeddable videos of a channel.
   *
   * @param channelId YouTube channel id.
   * @return up to the configured selection size of distinct videos, sorted by publication date
   *     descending and then by id; fewer (even none) when the channel does not have more.
   * @throws com.athlete.atlas.exception.RemoteException when the provider fails or returns
   *     inconsistent data.
   */
  List<YouTubeVideo> fetchLatestVideos(@Nonnull String channelId);
}
