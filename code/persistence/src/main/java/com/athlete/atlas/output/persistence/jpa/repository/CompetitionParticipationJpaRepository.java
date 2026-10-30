package com.athlete.atlas.output.persistence.jpa.repository;

import com.athlete.atlas.output.persistence.jpa.entity.CompetitionParticipationEntity;
import com.athlete.atlas.output.persistence.jpa.entity.CompetitionParticipationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  /**
   * Upsert in a single statement, without reading the row first, so there is no race between
   * checking and writing. Native because JPQL has no ON CONFLICT; it bypasses @Version, so the
   * version is incremented here. The final WHERE skips the write when the row is already confirmed.
   */
  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          INSERT INTO competition_participation (athlete_id, competition_id, confirmed)
          SELECT a.id, :competitionId, true FROM athlete a WHERE a.code = :athleteCode
          ON CONFLICT (athlete_id, competition_id)
          DO UPDATE SET confirmed = true, updated_at = now(),
              version = competition_participation.version + 1
          WHERE competition_participation.confirmed = false
          """)
  int confirm(
      @Param("athleteCode") String athleteCode, @Param("competitionId") String competitionId);

  /** Withdraws only a confirmed row; without a row there is nothing to withdraw. */
  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          UPDATE competition_participation
          SET confirmed = false, updated_at = now(), version = version + 1
          WHERE athlete_id = (SELECT a.id FROM athlete a WHERE a.code = :athleteCode)
              AND competition_id = :competitionId
              AND confirmed = true
          """)
  int withdraw(
      @Param("athleteCode") String athleteCode, @Param("competitionId") String competitionId);
}
