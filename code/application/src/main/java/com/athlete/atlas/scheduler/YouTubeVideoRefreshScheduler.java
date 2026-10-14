package com.athlete.atlas.scheduler;

import com.athlete.atlas.port.input.youtube.RefreshYouTubeVideosUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic refresh of the YouTube video selections, replacing the GitHub Actions workflow of the
 * current website. The first run happens on startup so that the endpoint has data as soon as
 * possible; requests never call YouTube.
 */
@Component
@RequiredArgsConstructor
public class YouTubeVideoRefreshScheduler {

  private final RefreshYouTubeVideosUseCase refreshYouTubeVideosUseCase;

  @Scheduled(
      initialDelayString = "${youtube.refresh-initial-delay:0s}",
      fixedDelayString = "${youtube.refresh-interval:6h}")
  public void refresh() {
    refreshYouTubeVideosUseCase.refreshAll();
  }
}
