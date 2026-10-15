package com.athlete.atlas.port.output;

import jakarta.annotation.Nonnull;
import java.util.Set;

/** Participation of the athletes in the competitions of the calendar. */
public interface CompetitionParticipationRepository {

  /**
   * Ids of the competitions the athlete has confirmed. Any other competition is not confirmed.
   *
   * @throws com.athlete.atlas.exception.StorageUnavailableException if the storage does not respond.
   */
  Set<String> findConfirmedCompetitionIds(@Nonnull String athleteCode);
}
