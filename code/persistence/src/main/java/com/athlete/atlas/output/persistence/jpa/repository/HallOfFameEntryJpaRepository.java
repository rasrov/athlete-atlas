package com.athlete.atlas.output.persistence.jpa.repository;

import com.athlete.atlas.output.persistence.jpa.entity.HallOfFameEntryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface HallOfFameEntryJpaRepository extends JpaRepository<HallOfFameEntryEntity, Long> {

  /**
   * Results of the athlete with their images in a single query (no N+1). Resolves the athlete
   * through the unique index on code and reads its results through hall_of_fame_entry_uk.
   */
  @Transactional(readOnly = true)
  @Query(
      """
      select distinct e
      from HallOfFameEntryEntity e
      join AthleteEntity a on a.id = e.athleteId
      left join fetch e.images
      where a.code = :athleteCode
      order by e.competitionDate desc, e.id asc
      """)
  List<HallOfFameEntryEntity> findByAthleteCode(@Param("athleteCode") String athleteCode);
}
