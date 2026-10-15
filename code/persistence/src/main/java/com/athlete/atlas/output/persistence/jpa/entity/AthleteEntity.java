package com.athlete.atlas.output.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "athlete")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AthleteEntity {

  /** allocationSize matches INCREMENT BY of the sequence, so ids are reserved 50 at a time. */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "athlete_seq")
  @SequenceGenerator(name = "athlete_seq", sequenceName = "athlete_seq", allocationSize = 50)
  private Long id;

  @Column(name = "code", nullable = false, length = 128)
  private String code;

  @Column(name = "youtube_channel_id", length = 24)
  private String youTubeChannelId;

  @Column(name = "instagram_username", length = 30)
  private String instagramUsername;
}
