package com.athlete.atlas.port.output;

import com.athlete.atlas.model.athlete.Athlete;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface AthleteRepository {

  /**
   * @throws com.athlete.atlas.exception.StorageUnavailableException if the storage does not respond.
   */
  Optional<Athlete> findByCode(@Nonnull String code);

  /**
   * Returns every athlete, uncached; meant for the periodic refreshes, not for requests.
   *
   * @throws com.athlete.atlas.exception.StorageUnavailableException if the storage does not respond.
   */
  List<Athlete> findAll();
}
