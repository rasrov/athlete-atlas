package com.athlete.atlas.output.persistence.jpa;

import com.athlete.atlas.output.persistence.StorageCalls;
import com.athlete.atlas.output.persistence.jpa.repository.CompetitionParticipationJpaRepository;
import com.athlete.atlas.port.output.CompetitionParticipationRepository;
import jakarta.annotation.Nonnull;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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

  /**
   * The transaction is in the JPA repository, so the entry is evicted after the commit, and only if
   * the write succeeded. It is evicted rather than overwritten: with two concurrent writes, writing
   * the value could cache the one that lost in the database.
   */
  @Override
  @CacheEvict(cacheNames = CacheNames.CONFIRMED_COMPETITIONS, key = "#athleteCode")
  public void setConfirmed(
      @Nonnull final String athleteCode,
      @Nonnull final String competitionId,
      final boolean confirmed) {
    StorageCalls.call(
        () ->
            confirmed
                ? competitionParticipationJpaRepository.confirm(athleteCode, competitionId)
                : competitionParticipationJpaRepository.withdraw(athleteCode, competitionId));
  }
}
