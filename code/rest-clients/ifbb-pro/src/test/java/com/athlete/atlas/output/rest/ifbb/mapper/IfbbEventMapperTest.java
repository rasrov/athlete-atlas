package com.athlete.atlas.output.rest.ifbb.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.enums.ParticipationStatus;
import com.athlete.atlas.output.rest.ifbb.generated.model.Event;
import com.athlete.atlas.output.rest.ifbb.generated.model.EventVenue;
import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

class IfbbEventMapperTest {

  private final IfbbEventMapper mapper = Mappers.getMapper(IfbbEventMapper.class);

  @Test
  void mapsAProviderEventToTheDomain() {
    final var competition =
        mapper.toDomain(event().venue(new EventVenue().city("Madrid").country("Spain")));

    assertThat(competition)
        .isEqualTo(
            new AthleteCompetition(
                "ifbb-12",
                "2027 Example & Pro",
                LocalDate.of(2027, 9, 30),
                LocalDate.of(2027, 10, 1),
                "Madrid",
                "Spain",
                "Classic Physique Men’s Physique",
                URI.create("https://www.ifbbpro.com/competition/example/"),
                "12",
                null,
                ParticipationStatus.PENDING));
  }

  @Test
  void keepsAnEmptyDescription() {
    assertThat(mapper.toDomain(event().description("")).description()).isEmpty();
  }

  @Test
  void aMissingVenueLeavesCityAndCountryNull() {
    final var competition = mapper.toDomain(event().venue(null));

    assertThat(competition.city()).isNull();
    assertThat(competition.country()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "<p> </p>"})
  void blankCityAndCountryBecomeNull(final String blank) {
    final var competition =
        mapper.toDomain(event().venue(new EventVenue().city(blank).country(blank)));

    assertThat(competition.city()).isNull();
    assertThat(competition.country()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-02-30 00:00:00", "2026-11-01", "2026-11-01T00:00:00", "invalid"})
  void rejectsDatesThatAreNotStrictLocalDateTimes(final String date) {
    assertThatThrownBy(() -> mapper.toDomain(event().startDate(date)))
        .isInstanceOf(DateTimeException.class);
  }

  private static Event event() {
    return new Event()
        .id(12L)
        .title("2027 Example &amp; Pro")
        .description("<p>Classic Physique<br />Men&#8217;s Physique</p>")
        .url(URI.create("https://www.ifbbpro.com/competition/example/"))
        .categories(List.of())
        .startDate("2027-09-30 00:00:00")
        .endDate("2027-10-01 23:59:59");
  }
}
