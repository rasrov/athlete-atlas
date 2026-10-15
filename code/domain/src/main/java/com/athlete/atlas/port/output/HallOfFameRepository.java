package com.athlete.atlas.port.output;

import com.athlete.atlas.model.halloffame.HallOfFameEntry;
import jakarta.annotation.Nonnull;
import java.util.List;

public interface HallOfFameRepository {

  /**
   * Returns the results of the athlete ordered by date descending, then by creation order, with
   * their images in editorial order. An unknown athlete has no results.
   *
   * @throws com.athlete.atlas.exception.StorageUnavailableException if the storage does not respond.
   */
  List<HallOfFameEntry> findByAthleteCode(@Nonnull String athleteCode);
}
