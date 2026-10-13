package com.athlete.atlas.input.rest.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.handler.GlobalExceptionHandler;
import com.athlete.atlas.input.rest.mapper.AthleteCompetitionMapperImpl;
import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.enums.ParticipationStatus;
import com.athlete.atlas.port.input.competition.AthleteCompetitionUseCase;
import java.net.URI;
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

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AthleteCompetitionUseCase athleteCompetitionUseCase;

  @Test
  void returnsTheCompetitionsWithTheContractShape() throws Exception {
    when(athleteCompetitionUseCase.listAthleteCompetitions(ATHLETE_ID, null))
        .thenReturn(List.of(competition()));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
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
        .andExpect(jsonPath("$.data[0].participation.status").value("pending"))
        .andExpect(jsonPath("$.meta.athleteId").value(ATHLETE_ID))
        .andExpect(jsonPath("$.meta", hasKey("updatedAt")))
        .andExpect(jsonPath("$.meta.updatedAt").value(nullValue()))
        .andExpect(jsonPath("$.meta.availability").value("unknown"));
  }

  @Test
  void passesTheParticipationStatusFilterToTheUseCase() throws Exception {
    when(athleteCompetitionUseCase.listAthleteCompetitions(
            ATHLETE_ID, ParticipationStatus.CONFIRMED))
        .thenReturn(List.of());

    mockMvc
        .perform(get(URL, ATHLETE_ID).queryParam("participationStatus", "confirmed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());

    verify(athleteCompetitionUseCase)
        .listAthleteCompetitions(ATHLETE_ID, ParticipationStatus.CONFIRMED);
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
        .andExpect(jsonPath("$.detail").value("Invalid value for parameters: athleteId."));

    verify(athleteCompetitionUseCase, never()).listAthleteCompetitions(anyString(), any());
  }

  @Test
  void rejectsAnUnknownParticipationStatusWithAProblem() throws Exception {
    mockMvc
        .perform(get(URL, ATHLETE_ID).queryParam("participationStatus", "foo"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(
            jsonPath("$.detail").value("Invalid value for parameters: participationStatus."));

    verify(athleteCompetitionUseCase, never()).listAthleteCompetitions(anyString(), any());
  }

  @Test
  void answersServiceUnavailableWhenTheProviderFails() throws Exception {
    when(athleteCompetitionUseCase.listAthleteCompetitions(ATHLETE_ID, null))
        .thenThrow(
            new RemoteException("[ifbb_events_api] upstream error", 502, new RuntimeException()));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Data temporarily unavailable"))
        .andExpect(jsonPath("$.status").value(503));
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
        null,
        ParticipationStatus.PENDING);
  }
}
