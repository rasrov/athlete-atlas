package com.athlete.atlas.input.rest.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.handler.GlobalExceptionHandler;
import com.athlete.atlas.input.rest.mapper.AthleteYouTubeVideoMapperImpl;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.port.input.youtube.AthleteYouTubeVideoUseCase;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(YouTubeVideoController.class)
@Import({AthleteYouTubeVideoMapperImpl.class, GlobalExceptionHandler.class})
class YouTubeVideoControllerTest {

  private static final String URL = "/api/v1/athletes/{athleteId}/youtube/videos";

  private static final String ATHLETE_ID = "kim-angel";

  private static final Instant UPDATED_AT = Instant.parse("2026-10-14T06:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AthleteYouTubeVideoUseCase athleteYouTubeVideoUseCase;

  @Test
  void returnsTheSelectionWithTheContractShape() throws Exception {
    when(athleteYouTubeVideoUseCase.listAthleteYouTubeVideos(ATHLETE_ID))
        .thenReturn(selection(6, Availability.AVAILABLE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.length()").value(6))
        .andExpect(jsonPath("$.data[0].id").value("AbCdEfGhI01"))
        .andExpect(jsonPath("$.data[0].title").value("Video 1"))
        .andExpect(jsonPath("$.data[0].publishedAt").value("2026-10-13T12:00:00Z"))
        .andExpect(
            jsonPath("$.data[0].watchUrl").value("https://www.youtube.com/watch?v=AbCdEfGhI01"))
        .andExpect(
            jsonPath("$.data[0].embedUrl")
                .value("https://www.youtube-nocookie.com/embed/AbCdEfGhI01"))
        .andExpect(jsonPath("$.data[0]", hasKey("thumbnailUrl")))
        .andExpect(jsonPath("$.data[0].thumbnailUrl").value(nullValue()))
        .andExpect(jsonPath("$.data[5].id").value("AbCdEfGhI06"))
        .andExpect(jsonPath("$.meta.athleteId").value(ATHLETE_ID))
        .andExpect(jsonPath("$.meta.updatedAt").value("2026-10-14T06:00:00Z"))
        .andExpect(jsonPath("$.meta.availability").value("available"));
  }

  @Test
  void returnsFewerThanSixVideosAsAValidResponse() throws Exception {
    when(athleteYouTubeVideoUseCase.listAthleteYouTubeVideos(ATHLETE_ID))
        .thenReturn(selection(2, Availability.AVAILABLE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2));
  }

  @Test
  void returnsAnEmptySelectionAsAValidResponse() throws Exception {
    when(athleteYouTubeVideoUseCase.listAthleteYouTubeVideos(ATHLETE_ID))
        .thenReturn(selection(0, Availability.AVAILABLE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void exposesAStaleSelection() throws Exception {
    when(athleteYouTubeVideoUseCase.listAthleteYouTubeVideos(ATHLETE_ID))
        .thenReturn(selection(6, Availability.STALE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.availability").value("stale"));
  }

  @Test
  void anAthleteWithoutChannelIsNotFound() throws Exception {
    when(athleteYouTubeVideoUseCase.listAthleteYouTubeVideos("unknown"))
        .thenThrow(new NotFoundException("Athlete without YouTube channel: unknown"));

    mockMvc
        .perform(get(URL, "unknown"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void answersServiceUnavailableUntilTheFirstSynchronization() throws Exception {
    when(athleteYouTubeVideoUseCase.listAthleteYouTubeVideos(ATHLETE_ID))
        .thenThrow(new DataUnavailableException("not synchronized"));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Data temporarily unavailable"))
        .andExpect(jsonPath("$.status").value(503));
  }

  @Test
  void rejectsAnInvalidAthleteIdWithAProblem() throws Exception {
    mockMvc
        .perform(get(URL, "kim$angel"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Invalid value for parameters: athleteId."));

    verify(athleteYouTubeVideoUseCase, never()).listAthleteYouTubeVideos(anyString());
  }

  private static AthleteSelection<YouTubeVideo> selection(final int count, final Availability availability) {
    final List<YouTubeVideo> videos =
        IntStream.rangeClosed(1, count)
            .mapToObj(
                n -> {
                  final var id = "AbCdEfGhI%02d".formatted(n);
                  return new YouTubeVideo(
                      id,
                      "Video " + n,
                      Instant.parse("2026-10-13T12:00:00Z").minusSeconds(3600L * (n - 1)),
                      URI.create("https://www.youtube.com/watch?v=" + id),
                      URI.create("https://www.youtube-nocookie.com/embed/" + id),
                      null);
                })
            .toList();
    return new AthleteSelection<>(ATHLETE_ID, videos, UPDATED_AT, availability);
  }
}
