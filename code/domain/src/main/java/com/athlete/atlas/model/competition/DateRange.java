package com.athlete.atlas.model.competition;

import jakarta.annotation.Nonnull;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.util.Objects;

/** Half-open date range: {@code start} is included, {@code endExclusive} is not. */
public record DateRange(@Nonnull LocalDate start, @Nonnull LocalDate endExclusive) {

  private static final MonthDay SEASON_START = MonthDay.of(Month.OCTOBER, 1);

  public DateRange {
    Objects.requireNonNull(start, "start must not be null");
    Objects.requireNonNull(endExclusive, "endExclusive must not be null");
    if (!start.isBefore(endExclusive)) {
      throw new IllegalArgumentException(
          "start (" + start + ") must be before endExclusive (" + endExclusive + ")");
    }
  }

  /** Season containing the given date: from October 1 to October 1 of the following year. */
  public static DateRange seasonOf(@Nonnull final LocalDate date) {
    final var seasonStartThisYear = SEASON_START.atYear(date.getYear());
    final var start =
        date.isBefore(seasonStartThisYear) ? seasonStartThisYear.minusYears(1) : seasonStartThisYear;
    return new DateRange(start, start.plusYears(1));
  }

  /** Last day included in the range. */
  public LocalDate endInclusive() {
    return endExclusive.minusDays(1);
  }

  /** Whether the inclusive period {@code [periodStart, periodEnd]} shares at least one day. */
  public boolean overlaps(@Nonnull final LocalDate periodStart, @Nonnull final LocalDate periodEnd) {
    return !periodEnd.isBefore(start) && periodStart.isBefore(endExclusive);
  }
}
