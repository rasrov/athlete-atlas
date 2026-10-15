package com.athlete.atlas.port.input.competition;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.enums.ParticipationStatus;
import com.athlete.atlas.model.selection.AthleteSelection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface AthleteCompetitionUseCase {

  /**
   * Returns the stored competitions of the current season; it does not call the provider.
   *
   * @throws com.athlete.atlas.exception.DataUnavailableException if no refresh of the current season
   *     has succeeded yet.
   */
  AthleteSelection<AthleteCompetition> listAthleteCompetitions(
      @Nonnull String athleteId, @Nullable ParticipationStatus participationStatus);
}
