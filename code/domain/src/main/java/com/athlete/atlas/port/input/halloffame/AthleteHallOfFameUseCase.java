package com.athlete.atlas.port.input.halloffame;

import com.athlete.atlas.model.halloffame.HallOfFameEntry;
import com.athlete.atlas.model.selection.AthleteSelection;
import jakarta.annotation.Nonnull;

public interface AthleteHallOfFameUseCase {

  /**
   * Returns every competition result of the athlete. The results are editorial data, so they are
   * always available; updatedAt is their last modification, or null when there are none.
   *
   * @throws com.athlete.atlas.exception.NotFoundException if the athlete does not exist.
   * @throws com.athlete.atlas.exception.StorageUnavailableException if the storage does not respond.
   */
  AthleteSelection<HallOfFameEntry> listAthleteHallOfFame(@Nonnull String athleteId);
}
