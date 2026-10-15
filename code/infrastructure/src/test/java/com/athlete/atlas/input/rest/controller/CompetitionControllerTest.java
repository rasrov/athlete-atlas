package com.athlete.atlas.input.rest.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.handler.GlobalExceptionHandler;
import com.athlete.atlas.input.rest.mapper.AthleteCompetitionMapperImpl;
import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.port.input.competition.AthleteCompetitionUseCase;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompetitionController.class)
@Import({AthleteCompetitionMapperImpl.class, GlobalExceptionHandler.class})
class CompetitionControllerTest {

  private static final String URL = "/api/v1/athletes/{athleteId}/competitions";

  private static final String ATHLETE_ID = "kim-angel";

  private static final Instant UPDATED_AT = Instant.parse("2026-10-14T06:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AthleteCompetitionUseCase athleteCompetitionUseCase;

  @Test
  void returnsTheCompetitionsWithTheContractShape() throws Exception {
    when(athleteCompetitionUseCase.listAthleteCompetitions(ATHLETE_ID))
        .thenReturn(selection(List.of(competition()), Availability.AVAILABLE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(
            header()
                .string(
                    "Cache-Control",
                    "max-age=300, public, stale-if-error=3600, stale-while-revalidate=60"))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].id").value("ifbb-12"))
        .andExpect(jsonPath("$.data[0].name").value("Example Pro"))
        .andExpect(jsonPath("$.data[0].startDate").value("2026-11-01"))
        .andExpect(jsonPath("$.data[0].endDate").value("2026-11-02"))
        .andExpect(jsonPath("$.data[0].city").value("Madrid"))
        .andExpect(jsonPath("$.data[0].country").value("Spain"))
        .andExpect(jsonPath("$.data[0].description").value("Classic Physique"))
        .andExpect(
            jsonPath("$.data[0].sourceUrl")
                .value("https://www.ifbbpro.com/competition/example/"))
        .andExpect(jsonPath("$.data[0].sourceId").value("12"))
        .andExpect(jsonPath("$.data[0]", hasKey("logoUrl")))
        .andExpect(jsonPath("$.data[0].logoUrl").value(nullValue()))
        .andExpect(jsonPath("$.data[0].participation").doesNotExist())
        .andExpect(jsonPath("$.meta.athleteId").value(ATHLETE_ID))
        .andExpect(jsonPath("$.meta.updatedAt").value("2026-10-14T06:00:00Z"))
        .andExpect(jsonPath("$.meta.availability").value("available"));
  }

  @Test
  void anAthleteWithoutConfirmedCompetitionsGetsAnEmptyList() throws Exception {
    when(athleteCompetitionUseCase.listAthleteCompetitions(ATHLETE_ID))
        .thenReturn(selection(List.of(), Availability.STALE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.meta.availability").value("stale"));
  }

  @Test
  void aDegradedResponseOfUnknownAvailabilityIsNotCached() throws Exception {
    when(athleteCompetitionUseCase.listAthleteCompetitions(ATHLETE_ID))
        .thenReturn(new AthleteSelection<>(ATHLETE_ID, List.of(), null, Availability.UNKNOWN));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.meta.updatedAt").value(nullValue()))
        .andExpect(jsonPath("$.meta.availability").value("unknown"));
  }

  @Test
  void rejectsAnInvalidAthleteIdWithAProblem() throws Exception {
    mockMvc
        .perform(get(URL, "kim$angel"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.title").value("Invalid parameters"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Invalid value for parameters: athleteId."))
        .andExpect(header().string("Cache-Control", "no-store"));

    verify(athleteCompetitionUseCase, never()).listAthleteCompetitions(anyString());
  }

  @Test
  void answersServiceUnavailableUntilTheFirstSynchronizationWithoutCaching() throws Exception {
    when(athleteCompetitionUseCase.listAthleteCompetitions(ATHLETE_ID))
        .thenThrow(new DataUnavailableException("not synchronized"));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.title").value("Data temporarily unavailable"))
        .andExpect(jsonPath("$.status").value(503));
  }

  private static AthleteSelection<AthleteCompetition> selection(
      final List<AthleteCompetition> competitions, final Availability availability) {
    return new AthleteSelection<>(ATHLETE_ID, competitions, UPDATED_AT, availability);
  }

  private static AthleteCompetition competition() {
    return new AthleteCompetition(
        "ifbb-12",
        "Example Pro",
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 11, 2),
        "Madrid",
        "Spain",
        "Classic Physique",
        URI.create("https://www.ifbbpro.com/competition/example/"),
        "12",
        null);
  }
}
