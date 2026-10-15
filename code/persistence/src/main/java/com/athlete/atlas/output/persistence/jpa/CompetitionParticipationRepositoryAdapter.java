package com.athlete.atlas.output.persistence.jpa;

import com.athlete.atlas.output.persistence.StorageCalls;
import com.athlete.atlas.output.persistence.jpa.repository.CompetitionParticipationJpaRepository;
import com.athlete.atlas.port.output.CompetitionParticipationRepository;
import jakarta.annotation.Nonnull;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompetitionParticipationRepositoryAdapter
    implements CompetitionParticipationRepository {

  private final CompetitionParticipationJpaRepository competitionParticipationJpaRepository;

  /**
   * Cached per athlete as an immutable set. sync: on a miss, a single request loads the value and
   * the others wait for it. A failure is not cached.
   */
  @Override
  @Cacheable(cacheNames = CacheNames.CONFIRMED_COMPETITIONS, sync = true)
  public Set<String> findConfirmedCompetitionIds(@Nonnull final String athleteCode) {
    return StorageCalls.call(
        () ->
            Set.copyOf(
                competitionParticipationJpaRepository.findConfirmedCompetitionIds(athleteCode)));
  }
}
