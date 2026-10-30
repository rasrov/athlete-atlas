package com.athlete.atlas.port.input.competition;

import jakarta.annotation.Nonnull;

public interface SetCompetitionParticipationUseCase {

  /**
   * Confirms or withdraws the participation of the athlete in a competition of the current season.
   * Idempotent: repeating it has the same effect, and withdrawing a competition that was never
   * confirmed changes nothing.
   *
   * @throws com.athlete.atlas.exception.NotFoundException if the athlete does not exist or the
   *     competition is not in the catalog of the current season.
   * @throws com.athlete.atlas.exception.DataUnavailableException if no refresh of the current season
   *     has succeeded yet.
   * @throws com.athlete.atlas.exception.StorageUnavailableException if the storage does not respond.
   */
  void setParticipation(
      @Nonnull String athleteId, @Nonnull String competitionId, boolean confirmed);
}
