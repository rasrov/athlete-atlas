package com.athlete.atlas.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.athlete.atlas.model.competition.DateRange;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

class CompetitionRefreshSchedulerTest {

  private final CronExpression seasonStart =
      CronExpression.parse(CompetitionRefreshScheduler.SEASON_START_CRON);

  @Test
  void runsOnceAYearAtMidnightOfOctoberFirst() {
    assertThat(seasonStart.next(LocalDateTime.of(2026, 3, 15, 12, 0)))
        .isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
    assertThat(seasonStart.next(LocalDateTime.of(2026, 10, 1, 0, 0)))
        .isEqualTo(LocalDateTime.of(2027, 10, 1, 0, 0));
  }

  @Test
  void runsWhenTheNewSeasonHasAlreadyBegun() {
    final var run = seasonStart.next(LocalDateTime.of(2026, 9, 30, 23, 59));

    assertThat(DateRange.seasonOf(run.toLocalDate()).start()).isEqualTo(LocalDate.of(2026, 10, 1));
  }
}
