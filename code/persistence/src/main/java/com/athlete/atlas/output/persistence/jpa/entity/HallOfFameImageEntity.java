package com.athlete.atlas.output.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "hall_of_fame_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HallOfFameImageEntity {

  /** allocationSize matches INCREMENT BY of the sequence, so ids are reserved 50 at a time. */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hall_of_fame_image_seq")
  @SequenceGenerator(
      name = "hall_of_fame_image_seq",
      sequenceName = "hall_of_fame_image_seq",
      allocationSize = 50)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entry_id", nullable = false)
  private HallOfFameEntryEntity entry;

  /** Editorial order of the image within its result. */
  @Column(name = "position", nullable = false)
  private short position;

  @Column(name = "url", nullable = false, columnDefinition = "text")
  private String url;

  @Column(name = "alt_text", columnDefinition = "text")
  private String altText;
}
