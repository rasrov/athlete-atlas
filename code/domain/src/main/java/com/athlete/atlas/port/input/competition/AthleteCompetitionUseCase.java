package com.athlete.atlas.port.input.competition;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.selection.AthleteSelection;
import jakarta.annotation.Nonnull;

public interface AthleteCompetitionUseCase {

  /**
   * Returns the competitions of the current season in which the athlete participates (confirmed);
   * it does not call the provider.
   *
   * @throws com.athlete.atlas.exception.NotFoundException if the athlete does not exist.
   * @throws com.athlete.atlas.exception.DataUnavailableException if no refresh of the current season
   *     has succeeded yet.
   */
  AthleteSelection<AthleteCompetition> listAthleteCompetitions(@Nonnull String athleteId);
}
