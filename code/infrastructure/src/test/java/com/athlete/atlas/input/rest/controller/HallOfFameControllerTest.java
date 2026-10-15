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

import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.handler.GlobalExceptionHandler;
import com.athlete.atlas.input.rest.mapper.AthleteHallOfFameMapperImpl;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.halloffame.HallOfFameEntry;
import com.athlete.atlas.model.halloffame.HallOfFameImage;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.port.input.halloffame.AthleteHallOfFameUseCase;
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

@WebMvcTest(HallOfFameController.class)
@Import({AthleteHallOfFameMapperImpl.class, GlobalExceptionHandler.class})
class HallOfFameControllerTest {

  private static final String URL = "/api/v1/athletes/{athleteId}/hall-of-fame";

  private static final String ATHLETE_ID = "kim-angel";

  private static final Instant UPDATED_AT = Instant.parse("2026-10-15T18:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AthleteHallOfFameUseCase athleteHallOfFameUseCase;

  @Test
  void returnsTheResultsWithTheContractShape() throws Exception {
    final var withImages =
        new HallOfFameEntry(
            "51",
            LocalDate.of(2026, 9, 12),
            "Classic Physique",
            "Example Pro Show",
            "5.º Puesto",
            URI.create("https://www.ifbbpro.com/competition/example-pro-show/"),
            "ifbb-12345",
            List.of(
                new HallOfFameImage(
                    URI.create("https://images.example.com/1.webp"), "On stage"),
                new HallOfFameImage(URI.create("https://images.example.com/2.webp"), null)),
            UPDATED_AT);
    final var withoutOptionals =
        new HallOfFameEntry(
            "1",
            LocalDate.of(2025, 11, 8),
            "Classic Physique",
            "Example Championship",
            "16.º Top mundial",
            null,
            null,
            List.of(),
            UPDATED_AT.minusSeconds(3600));
    when(athleteHallOfFameUseCase.listAthleteHallOfFame(ATHLETE_ID))
        .thenReturn(
            new AthleteSelection<>(
                ATHLETE_ID, List.of(withImages, withoutOptionals), UPDATED_AT, Availability.AVAILABLE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Cache-Control",
                    "max-age=300, public, stale-if-error=3600, stale-while-revalidate=60"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].id").value("51"))
        .andExpect(jsonPath("$.data[0].date").value("2026-09-12"))
        .andExpect(jsonPath("$.data[0].category").value("Classic Physique"))
        .andExpect(jsonPath("$.data[0].competitionName").value("Example Pro Show"))
        .andExpect(jsonPath("$.data[0].placement").value("5.º Puesto"))
        .andExpect(
            jsonPath("$.data[0].officialUrl")
                .value("https://www.ifbbpro.com/competition/example-pro-show/"))
        .andExpect(jsonPath("$.data[0].competitionId").value("ifbb-12345"))
        .andExpect(jsonPath("$.data[0].images.length()").value(2))
        .andExpect(jsonPath("$.data[0].images[0].url").value("https://images.example.com/1.webp"))
        .andExpect(jsonPath("$.data[0].images[0].altText").value("On stage"))
        .andExpect(jsonPath("$.data[0].images[1]", hasKey("altText")))
        .andExpect(jsonPath("$.data[0].images[1].altText").value(nullValue()))
        .andExpect(jsonPath("$.data[0].updatedAt").doesNotExist())
        .andExpect(jsonPath("$.data[1].id").value("1"))
        .andExpect(jsonPath("$.data[1]", hasKey("officialUrl")))
        .andExpect(jsonPath("$.data[1].officialUrl").value(nullValue()))
        .andExpect(jsonPath("$.data[1]", hasKey("competitionId")))
        .andExpect(jsonPath("$.data[1].competitionId").value(nullValue()))
        .andExpect(jsonPath("$.data[1].images").isEmpty())
        .andExpect(jsonPath("$.meta.athleteId").value(ATHLETE_ID))
        .andExpect(jsonPath("$.meta.updatedAt").value("2026-10-15T18:00:00Z"))
        .andExpect(jsonPath("$.meta.availability").value("available"));
  }

  @Test
  void anAthleteWithoutResultsIsAValidResponse() throws Exception {
    when(athleteHallOfFameUseCase.listAthleteHallOfFame(ATHLETE_ID))
        .thenReturn(new AthleteSelection<>(ATHLETE_ID, List.of(), null, Availability.AVAILABLE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.meta", hasKey("updatedAt")))
        .andExpect(jsonPath("$.meta.updatedAt").value(nullValue()));
  }

  @Test
  void anUnknownAthleteIsNotFound() throws Exception {
    when(athleteHallOfFameUseCase.listAthleteHallOfFame("unknown"))
        .thenThrow(new NotFoundException("Athlete not found: unknown"));

    mockMvc
        .perform(get(URL, "unknown"))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void answersServiceUnavailableWhenTheStorageDoesNotRespond() throws Exception {
    when(athleteHallOfFameUseCase.listAthleteHallOfFame(ATHLETE_ID))
        .thenThrow(new StorageUnavailableException("Database unavailable", null));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(503));
  }

  @Test
  void rejectsAnInvalidAthleteIdWithAProblem() throws Exception {
    mockMvc
        .perform(get(URL, "kim$angel"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

    verify(athleteHallOfFameUseCase, never()).listAthleteHallOfFame(anyString());
  }
}
