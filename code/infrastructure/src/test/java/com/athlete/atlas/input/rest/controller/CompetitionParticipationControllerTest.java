package com.athlete.atlas.input.rest.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.handler.GlobalExceptionHandler;
import com.athlete.atlas.port.input.competition.SetCompetitionParticipationUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompetitionParticipationController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "api.write-enabled=true")
class CompetitionParticipationControllerTest {

  private static final String URL =
      "/api/v1/athletes/{athleteId}/competitions/{competitionId}/participation";

  private static final String ATHLETE_ID = "kim-angel";

  private static final String COMPETITION_ID = "ifbb-12";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SetCompetitionParticipationUseCase setCompetitionParticipationUseCase;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void setsTheParticipationWithoutBodyAndWithoutCaching(final boolean confirmed) throws Exception {
    mockMvc
        .perform(
            put(URL, ATHLETE_ID, COMPETITION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\": " + confirmed + "}"))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().string(""));

    verify(setCompetitionParticipationUseCase)
        .setParticipation(ATHLETE_ID, COMPETITION_ID, confirmed);
  }

  @Test
  void ignoresUnknownBodyFields() throws Exception {
    mockMvc
        .perform(
            put(URL, ATHLETE_ID, COMPETITION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\": true, \"comment\": \"see you there\"}"))
        .andExpect(status().isNoContent());

    verify(setCompetitionParticipationUseCase).setParticipation(ATHLETE_ID, COMPETITION_ID, true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"confirmed\": null}"})
  void rejectsABodyWithoutConfirmed(final String body) throws Exception {
    mockMvc
        .perform(
            put(URL, ATHLETE_ID, COMPETITION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Invalid parameters"))
        .andExpect(jsonPath("$.detail").value("Invalid value for parameters: confirmed."))
        .andExpect(header().string("Cache-Control", "no-store"));

    verifyNotCalled();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not json", "{\"confirmed\": \"maybe\"}", "{\"confirmed\": [true]}"})
  void rejectsAMissingOrMalformedBody(final String body) throws Exception {
    mockMvc
        .perform(
            put(URL, ATHLETE_ID, COMPETITION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Invalid parameters"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(header().string("Cache-Control", "no-store"));

    verifyNotCalled();
  }

  @Test
  void rejectsAContentTypeOtherThanJsonWithBadRequest() throws Exception {
    mockMvc
        .perform(
            put(URL, ATHLETE_ID, COMPETITION_ID)
                .contentType(MediaType.TEXT_PLAIN)
                .content("{\"confirmed\": true}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Invalid parameters"))
        .andExpect(header().string("Cache-Control", "no-store"));

    verifyNotCalled();
  }

  @Test
  void rejectsAnInvalidCompetitionId() throws Exception {
    mockMvc
        .perform(
            put(URL, ATHLETE_ID, "ifbb$12")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\": true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Invalid value for parameters: competitionId."));

    verifyNotCalled();
  }

  @Test
  void anUnknownAthleteOrCompetitionIsNotFound() throws Exception {
    doThrow(new NotFoundException("Competition not in the current season: ifbb-12"))
        .when(setCompetitionParticipationUseCase)
        .setParticipation(ATHLETE_ID, COMPETITION_ID, true);

    mockMvc
        .perform(
            put(URL, ATHLETE_ID, COMPETITION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\": true}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void answersServiceUnavailableUntilTheFirstSynchronization() throws Exception {
    doThrow(new DataUnavailableException("not synchronized"))
        .when(setCompetitionParticipationUseCase)
        .setParticipation(ATHLETE_ID, COMPETITION_ID, true);

    mockMvc
        .perform(
            put(URL, ATHLETE_ID, COMPETITION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\": true}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void answersServiceUnavailableWhenTheStorageDoesNotRespond() throws Exception {
    doThrow(new StorageUnavailableException("down", null))
        .when(setCompetitionParticipationUseCase)
        .setParticipation(ATHLETE_ID, COMPETITION_ID, true);

    mockMvc
        .perform(
            put(URL, ATHLETE_ID, COMPETITION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\": true}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.title").value("Data temporarily unavailable"));
  }

  private void verifyNotCalled() {
    verify(setCompetitionParticipationUseCase, never())
        .setParticipation(anyString(), anyString(), anyBoolean());
  }
}
