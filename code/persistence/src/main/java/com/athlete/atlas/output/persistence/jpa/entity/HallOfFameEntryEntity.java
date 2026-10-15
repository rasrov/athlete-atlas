package com.athlete.atlas.output.persistence.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Competition result of an athlete. The athlete is referenced by id, without an association. */
@Getter
@Entity
@Table(name = "hall_of_fame_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HallOfFameEntryEntity {

  /** allocationSize matches INCREMENT BY of the sequence, so ids are reserved 50 at a time. */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hall_of_fame_entry_seq")
  @SequenceGenerator(
      name = "hall_of_fame_entry_seq",
      sequenceName = "hall_of_fame_entry_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "athlete_id", nullable = false)
  private Long athleteId;

  @Column(name = "competition_date", nullable = false)
  private LocalDate competitionDate;

  @Column(name = "category", nullable = false, length = 100)
  private String category;

  @Column(name = "competition_name", nullable = false, length = 200)
  private String competitionName;

  @Column(name = "placement", nullable = false, length = 100)
  private String placement;

  @Column(name = "official_url", columnDefinition = "text")
  private String officialUrl;

  @Column(name = "competition_id", length = 128)
  private String competitionId;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private int version;

  /** The list received on a write is always the final state, so removed images are deleted. */
  @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<HallOfFameImageEntity> images = new ArrayList<>();
}
