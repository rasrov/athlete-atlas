package com.athlete.atlas.output.persistence.jpa;

import com.athlete.atlas.model.halloffame.HallOfFameEntry;
import com.athlete.atlas.model.halloffame.HallOfFameImage;
import com.athlete.atlas.output.persistence.StorageCalls;
import com.athlete.atlas.output.persistence.jpa.entity.HallOfFameEntryEntity;
import com.athlete.atlas.output.persistence.jpa.entity.HallOfFameImageEntity;
import com.athlete.atlas.output.persistence.jpa.repository.HallOfFameEntryJpaRepository;
import com.athlete.atlas.port.output.HallOfFameRepository;
import jakarta.annotation.Nonnull;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HallOfFameRepositoryAdapter implements HallOfFameRepository {

  private final HallOfFameEntryJpaRepository hallOfFameEntryJpaRepository;

  /**
   * Cached by athlete code. The entry will be evicted when the results are written; until then,
   * changes made outside the backend (SQL) are visible when the entry expires.
   */
  @Override
  @Cacheable(cacheNames = CacheNames.HALL_OF_FAME, sync = true)
  public List<HallOfFameEntry> findByAthleteCode(@Nonnull final String athleteCode) {
    return StorageCalls.call(
        () ->
            hallOfFameEntryJpaRepository.findByAthleteCode(athleteCode).stream()
                .map(HallOfFameRepositoryAdapter::toDomain)
                .toList());
  }

  private static HallOfFameEntry toDomain(final HallOfFameEntryEntity entity) {
    return new HallOfFameEntry(
        String.valueOf(entity.getId()),
        entity.getCompetitionDate(),
        entity.getCategory(),
        entity.getCompetitionName(),
        entity.getPlacement(),
        entity.getOfficialUrl() == null ? null : URI.create(entity.getOfficialUrl()),
        entity.getCompetitionId(),
        entity.getImages().stream().map(HallOfFameRepositoryAdapter::toDomain).toList(),
        entity.getUpdatedAt());
  }

  private static HallOfFameImage toDomain(final HallOfFameImageEntity entity) {
    return new HallOfFameImage(URI.create(entity.getUrl()), entity.getAltText());
  }
}
