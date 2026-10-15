package com.athlete.atlas.output.persistence.jpa.repository;

import com.athlete.atlas.output.persistence.jpa.entity.AthleteEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface AthleteJpaRepository extends JpaRepository<AthleteEntity, Long> {

  /** Uses the unique index on code. */
  @Transactional(readOnly = true)
  Optional<AthleteEntity> findByCode(String code);

  /** Ordered so that the refreshes always process the athletes in the same order. */
  @Transactional(readOnly = true)
  List<AthleteEntity> findAllByOrderByCodeAsc();
}
