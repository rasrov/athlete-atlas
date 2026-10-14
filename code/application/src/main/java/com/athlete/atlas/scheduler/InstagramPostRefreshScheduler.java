package com.athlete.atlas.scheduler;

import com.athlete.atlas.port.input.instagram.RefreshInstagramPostsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic refresh of the Instagram post selections, replacing the GitHub Actions workflow of the
 * current website. The first run happens on startup so that the endpoint has data as soon as
 * possible; requests never call Meta.
 */
@Component
@RequiredArgsConstructor
public class InstagramPostRefreshScheduler {

  private final RefreshInstagramPostsUseCase refreshInstagramPostsUseCase;

  @Scheduled(
      initialDelayString = "${instagram.refresh-initial-delay:0s}",
      fixedDelayString = "${instagram.refresh-interval:6h}")
  public void refresh() {
    refreshInstagramPostsUseCase.refreshAll();
  }
}
