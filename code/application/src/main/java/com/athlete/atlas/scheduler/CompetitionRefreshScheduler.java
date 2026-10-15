package com.athlete.atlas.scheduler;

import com.athlete.atlas.port.input.competition.RefreshCompetitionsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic refresh of the competition catalog. The first run happens on startup so that the
 * endpoint has data as soon as possible; requests never call the IFBB website, so a burst of
 * traffic cannot turn into provider calls.
 */
@Component
@RequiredArgsConstructor
public class CompetitionRefreshScheduler {

  /**
   * Midnight of October 1, when a new season begins (see DateRange.seasonOf). It runs in
   * app.time-zone, the same zone as the application Clock.
   */
  static final String SEASON_START_CRON = "0 0 0 1 10 *";

  private final RefreshCompetitionsUseCase refreshCompetitionsUseCase;

  @Scheduled(
      initialDelayString = "${competitions.refresh-initial-delay:0s}",
      fixedDelayString = "${competitions.refresh-interval:6h}")
  public void refresh() {
    refreshCompetitionsUseCase.refresh();
  }

  /**
   * The catalog of the previous season is not served, so the new one is fetched as soon as the
   * season begins instead of waiting for the next periodic refresh.
   */
  @Scheduled(
      cron = "${competitions.season-start-cron:" + SEASON_START_CRON + "}",
      zone = "${app.time-zone:Europe/Madrid}")
  public void refreshOnSeasonStart() {
    refreshCompetitionsUseCase.refresh();
  }
}
