package com.athlete.atlas.input.rest.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.athlete.atlas.handler.GlobalExceptionHandler;
import com.athlete.atlas.port.input.competition.SetCompetitionParticipationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Without api.write-enabled (the default) the write endpoint does not exist. */
@WebMvcTest(CompetitionParticipationController.class)
@Import(GlobalExceptionHandler.class)
class CompetitionParticipationDisabledTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SetCompetitionParticipationUseCase setCompetitionParticipationUseCase;

  @Test
  void theWriteEndpointIsNotRegisteredByDefault() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/athletes/kim-angel/competitions/ifbb-12/participation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmed\": true}"))
        .andExpect(status().isNotFound());

    verifyNoInteractions(setCompetitionParticipationUseCase);
  }
}
