package com.athlete.atlas.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.model.competition.enums.ParticipationStatus;
import com.athlete.atlas.port.output.CompetitionPort;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AthleteCompetitionUseCaseImplTest {

  private static final String ATHLETE_ID = "kim-angel";

  private CompetitionPort competitionPort;

  @BeforeEach
  void setUp() {
    competitionPort = mock(CompetitionPort.class);
  }

  @ParameterizedTest
  @CsvSource({
    "2026-09-30, 2025-10-01, 2026-10-01",
    "2026-10-01, 2026-10-01, 2027-10-01",
    "2027-03-15, 2026-10-01, 2027-10-01"
  })
  void fetchesTheSeasonOfTheCurrentDate(
      final LocalDate today, final LocalDate expectedStart, final LocalDate expectedEnd) {
    useCaseAt(today).listAthleteCompetitions(ATHLETE_ID, null);

    verify(competitionPort).fetchCompetitions(new DateRange(expectedStart, expectedEnd));
  }

  @Test
  void returnsTheCompetitionsFromThePort() {
    final var today = LocalDate.of(2026, 10, 13);
    final var competitions = List.of(competition());
    when(competitionPort.fetchCompetitions(DateRange.seasonOf(today))).thenReturn(competitions);

    final var result =
        useCaseAt(today).listAthleteCompetitions(ATHLETE_ID, ParticipationStatus.CONFIRMED);

    assertThat(result).isEqualTo(competitions);
  }

  private AthleteCompetitionUseCaseImpl useCaseAt(final LocalDate today) {
    final var clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    return new AthleteCompetitionUseCaseImpl(competitionPort, clock);
  }

  private static AthleteCompetition competition() {
    return new AthleteCompetition(
        "ifbb-1",
        "Example Pro",
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 11, 1),
        "Madrid",
        "Spain",
        "Classic Physique",
        URI.create("https://www.ifbbpro.com/competition/example/"),
        "1",
        null,
        ParticipationStatus.PENDING);
  }
}
