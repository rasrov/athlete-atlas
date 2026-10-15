package com.athlete.atlas.output.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** An athlete has a single participation row per competition. */
@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompetitionParticipationId implements Serializable {

  @Serial private static final long serialVersionUID = 4106925390138419514L;

  @Column(name = "athlete_id", nullable = false)
  private Long athleteId;

  /** API id of the competition (e.g. ifbb-12345); competitions are not stored in the database. */
  @Column(name = "competition_id", nullable = false, length = 128)
  private String competitionId;
}
