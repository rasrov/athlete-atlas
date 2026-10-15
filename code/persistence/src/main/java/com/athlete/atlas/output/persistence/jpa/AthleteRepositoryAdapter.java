package com.athlete.atlas.output.persistence.jpa;

import com.athlete.atlas.model.athlete.Athlete;
import com.athlete.atlas.output.persistence.StorageCalls;
import com.athlete.atlas.output.persistence.jpa.entity.AthleteEntity;
import com.athlete.atlas.output.persistence.jpa.repository.AthleteJpaRepository;
import com.athlete.atlas.port.output.AthleteRepository;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AthleteRepositoryAdapter implements AthleteRepository {

  private final AthleteJpaRepository athleteJpaRepository;

  /**
   * Cached, unknown codes included (the empty Optional is stored as null), so repeated requests for
   * the same code do not reach the database. sync: on a miss, a single request loads the value.
   */
  @Override
  @Cacheable(cacheNames = CacheNames.ATHLETES, sync = true)
  public Optional<Athlete> findByCode(@Nonnull final String code) {
    return StorageCalls.call(
        () -> athleteJpaRepository.findByCode(code).map(AthleteRepositoryAdapter::toDomain));
  }

  @Override
  public List<Athlete> findAll() {
    return StorageCalls.call(
        () ->
            athleteJpaRepository.findAllByOrderByCodeAsc().stream()
                .map(AthleteRepositoryAdapter::toDomain)
                .toList());
  }

  private static Athlete toDomain(final AthleteEntity entity) {
    return new Athlete(
        entity.getCode(), entity.getYouTubeChannelId(), entity.getInstagramUsername());
  }
}
