package com.athlete.atlas.port.input.competition;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.enums.ParticipationStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public interface AthleteCompetitionUseCase {

  List<AthleteCompetition> listAthleteCompetitions(
      @Nonnull String athleteId, @Nullable ParticipationStatus participationStatus);
}
