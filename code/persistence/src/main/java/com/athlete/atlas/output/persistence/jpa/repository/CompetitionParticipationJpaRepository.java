package com.athlete.atlas.output.persistence.jpa.repository;

import com.athlete.atlas.output.persistence.jpa.entity.CompetitionParticipationEntity;
import com.athlete.atlas.output.persistence.jpa.entity.CompetitionParticipationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CompetitionParticipationJpaRepository
    extends JpaRepository<CompetitionParticipationEntity, CompetitionParticipationId> {

  /**
   * Projection of the confirmed competition ids only. Resolves the athlete through the unique
   * index on code and reads its rows through the athlete_id prefix of the primary key.
   */
  @Transactional(readOnly = true)
  @Query(
      """
      select p.id.competitionId
      from CompetitionParticipationEntity p
      join AthleteEntity a on a.id = p.id.athleteId
      where a.code = :athleteCode and p.confirmed = true
      """)
  List<String> findConfirmedCompetitionIds(@Param("athleteCode") String athleteCode);
}
