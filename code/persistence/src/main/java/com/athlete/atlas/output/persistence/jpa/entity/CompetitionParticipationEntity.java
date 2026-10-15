package com.athlete.atlas.output.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Whether an athlete attends a competition. Rows are created only when the athlete confirms; a
 * competition without a row is not confirmed.
 */
@Getter
@Entity
@Table(name = "competition_participation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionParticipationEntity {

  @EmbeddedId private CompetitionParticipationId id;

  @Column(name = "confirmed", nullable = false)
  private boolean confirmed;

  @Version
  @Column(name = "version", nullable = false)
  private int version;
}
