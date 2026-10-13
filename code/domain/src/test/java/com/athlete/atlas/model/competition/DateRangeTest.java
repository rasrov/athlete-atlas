package com.athlete.atlas.model.competition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DateRangeTest {

  @ParameterizedTest
  @CsvSource({
    "2026-10-01, 2026-10-01, 2027-10-01",
    "2027-09-30, 2026-10-01, 2027-10-01",
    "2027-01-15, 2026-10-01, 2027-10-01",
    "2026-12-31, 2026-10-01, 2027-10-01",
    "2026-09-30, 2025-10-01, 2026-10-01"
  })
  void seasonOfStartsOnOctoberFirst(
      final LocalDate date, final LocalDate expectedStart, final LocalDate expectedEnd) {
    assertThat(DateRange.seasonOf(date)).isEqualTo(new DateRange(expectedStart, expectedEnd));
  }

  @Test
  void rejectsEmptyOrReversedRanges() {
    final var day = LocalDate.of(2026, 10, 1);
    assertThatThrownBy(() -> new DateRange(day, day)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DateRange(day, day.minusDays(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void endInclusiveIsTheDayBeforeEndExclusive() {
    final var range = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 10, 1));
    assertThat(range.endInclusive()).isEqualTo(LocalDate.of(2027, 9, 30));
  }

  @Test
  void overlapsTreatsTheEndAsExclusive() {
    final var range = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 10, 1));

    assertThat(range.overlaps(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1))).isTrue();
    assertThat(range.overlaps(LocalDate.of(2027, 9, 30), LocalDate.of(2027, 10, 2))).isTrue();
    assertThat(range.overlaps(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 30))).isFalse();
    assertThat(range.overlaps(LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 2))).isFalse();
  }
}
