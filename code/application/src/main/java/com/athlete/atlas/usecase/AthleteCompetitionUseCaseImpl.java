package com.athlete.atlas.usecase;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.model.competition.enums.ParticipationStatus;
import com.athlete.atlas.port.input.competition.AthleteCompetitionUseCase;
import com.athlete.atlas.port.output.CompetitionPort;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AthleteCompetitionUseCaseImpl implements AthleteCompetitionUseCase {

  private final CompetitionPort competitionPort;

  private final Clock clock;

  @Override
  public List<AthleteCompetition> listAthleteCompetitions(
      @Nonnull final String athleteId, @Nullable final ParticipationStatus participationStatus) {
    return competitionPort.fetchCompetitions(DateRange.seasonOf(LocalDate.now(clock)));
  }
}
