package com.athlete.atlas.input.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.athlete.atlas.infrastructure.api.model.Competition;
import com.athlete.atlas.infrastructure.api.model.Metadata;
import com.athlete.atlas.infrastructure.api.model.Participation;
import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.enums.ParticipationStatus;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.selection.AthleteSelection;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;

class AthleteCompetitionMapperTest {

  private final AthleteCompetitionMapper mapper = Mappers.getMapper(AthleteCompetitionMapper.class);

  @Test
  void mapsEveryFieldToTheDto() {
    final var dto = mapper.toDto(competition(ParticipationStatus.CONFIRMED, "Madrid", "Spain"));

    assertThat(dto)
        .isEqualTo(
            new Competition(
                "ifbb-12",
                "Example Pro",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 2),
                "Madrid",
                "Spain",
                "Classic Physique",
                URI.create("https://www.ifbbpro.com/competition/example/"),
                "12",
                URI.create("https://cdn.example.com/logo.png"),
                new Participation(
                    com.athlete.atlas.infrastructure.api.model.ParticipationStatus.CONFIRMED)));
  }

  @Test
  void keepsOptionalFieldsAsNull() {
    final var dto = mapper.toDto(competition(ParticipationStatus.PENDING, null, null));

    assertThat(dto.getCity()).isNull();
    assertThat(dto.getCountry()).isNull();
  }

  @ParameterizedTest
  @EnumSource(ParticipationStatus.class)
  void mapsEveryParticipationStatusBothWays(final ParticipationStatus status) {
    final var dtoStatus = mapper.toDto(competition(status, null, null)).getParticipation().getStatus();

    assertThat(dtoStatus.name()).isEqualTo(status.name());
    assertThat(mapper.toDomain(dtoStatus)).isEqualTo(status);
  }

  @Test
  void anAbsentParticipationStatusFilterIsNull() {
    assertThat(mapper.toDomain(null)).isNull();
  }

  @Test
  void buildsTheCollectionWithTheSynchronizationMetadata() {
    final var competition = competition(ParticipationStatus.PENDING, "Madrid", "Spain");

    final var collection =
        mapper.toDtoCollection(selection(List.of(competition), Availability.STALE));

    assertThat(collection.getData()).containsExactly(mapper.toDto(competition));
    assertThat(collection.getMeta())
        .isEqualTo(
            new Metadata(
                "kim-angel",
                OffsetDateTime.parse("2026-10-14T06:00:00Z"),
                Metadata.AvailabilityEnum.STALE));
  }

  @Test
  void anEmptySelectionIsAnEmptyCollection() {
    assertThat(mapper.toDtoCollection(selection(List.of(), Availability.AVAILABLE)).getData())
        .isEmpty();
  }

  private static AthleteSelection<AthleteCompetition> selection(
      final List<AthleteCompetition> competitions, final Availability availability) {
    return new AthleteSelection<>(
        "kim-angel", competitions, Instant.parse("2026-10-14T06:00:00Z"), availability);
  }

  private static AthleteCompetition competition(
      final ParticipationStatus status, final String city, final String country) {
    return new AthleteCompetition(
        "ifbb-12",
        "Example Pro",
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 11, 2),
        city,
        country,
        "Classic Physique",
        URI.create("https://www.ifbbpro.com/competition/example/"),
        "12",
        URI.create("https://cdn.example.com/logo.png"),
        status);
  }
}
