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
import com.athlete.atlas.input.rest.mapper.AthleteInstagramPostMapperImpl;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.instagram.InstagramMediaType;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.port.input.instagram.AthleteInstagramPostUseCase;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InstagramPostController.class)
@Import({AthleteInstagramPostMapperImpl.class, GlobalExceptionHandler.class})
class InstagramPostControllerTest {

  private static final String URL = "/api/v1/athletes/{athleteId}/instagram/posts";

  private static final String ATHLETE_ID = "kim-angel";

  private static final Instant UPDATED_AT = Instant.parse("2026-10-14T06:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AthleteInstagramPostUseCase athleteInstagramPostUseCase;

  @Test
  void returnsTheSelectionWithTheContractShape() throws Exception {
    when(athleteInstagramPostUseCase.listAthleteInstagramPosts(ATHLETE_ID))
        .thenReturn(
            selection(
                List.of(
                    post("3", InstagramMediaType.VIDEO, "https://x.cdninstagram.com/t.jpg"),
                    post("2", InstagramMediaType.IMAGE, null),
                    post("1", InstagramMediaType.CAROUSEL_ALBUM, null)),
                Availability.AVAILABLE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.length()").value(3))
        .andExpect(jsonPath("$.data[0].id").value("3"))
        .andExpect(jsonPath("$.data[0].mediaType").value("VIDEO"))
        .andExpect(jsonPath("$.data[0].publishedAt").value("2026-10-13T10:00:00Z"))
        .andExpect(jsonPath("$.data[0].permalink").value("https://www.instagram.com/p/C3/"))
        .andExpect(jsonPath("$.data[0].previewImageUrl").value("https://x.cdninstagram.com/t.jpg"))
        .andExpect(jsonPath("$.data[1]", hasKey("previewImageUrl")))
        .andExpect(jsonPath("$.data[1].previewImageUrl").value(nullValue()))
        .andExpect(jsonPath("$.data[2].mediaType").value("CAROUSEL_ALBUM"))
        .andExpect(jsonPath("$.meta.athleteId").value(ATHLETE_ID))
        .andExpect(jsonPath("$.meta.updatedAt").value("2026-10-14T06:00:00Z"))
        .andExpect(jsonPath("$.meta.availability").value("available"));
  }

  @Test
  void returnsFewerThanThreePostsAsAValidResponse() throws Exception {
    when(athleteInstagramPostUseCase.listAthleteInstagramPosts(ATHLETE_ID))
        .thenReturn(
            selection(List.of(post("1", InstagramMediaType.IMAGE, null)), Availability.STALE));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.meta.availability").value("stale"));
  }

  @Test
  void anAthleteWithoutProfileIsNotFound() throws Exception {
    when(athleteInstagramPostUseCase.listAthleteInstagramPosts("unknown"))
        .thenThrow(new NotFoundException("Athlete without Instagram account: unknown"));

    mockMvc
        .perform(get(URL, "unknown"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void answersServiceUnavailableUntilTheFirstSynchronization() throws Exception {
    when(athleteInstagramPostUseCase.listAthleteInstagramPosts(ATHLETE_ID))
        .thenThrow(new DataUnavailableException("not synchronized"));

    mockMvc
        .perform(get(URL, ATHLETE_ID))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Data temporarily unavailable"));
  }

  @Test
  void rejectsAnInvalidAthleteIdWithAProblem() throws Exception {
    mockMvc
        .perform(get(URL, "kim$angel"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Invalid value for parameters: athleteId."));

    verify(athleteInstagramPostUseCase, never()).listAthleteInstagramPosts(anyString());
  }

  private static AthleteSelection<InstagramPost> selection(
      final List<InstagramPost> posts, final Availability availability) {
    return new AthleteSelection<>(ATHLETE_ID, posts, UPDATED_AT, availability);
  }

  private static InstagramPost post(
      final String id, final InstagramMediaType mediaType, final String preview) {
    return new InstagramPost(
        id,
        mediaType,
        Instant.parse("2026-10-13T10:00:00Z"),
        URI.create("https://www.instagram.com/p/C" + id + "/"),
        preview == null ? null : URI.create(preview));
  }
}
