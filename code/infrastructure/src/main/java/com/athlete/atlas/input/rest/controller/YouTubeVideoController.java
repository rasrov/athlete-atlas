package com.athlete.atlas.input.rest.controller;

import com.athlete.atlas.infrastructure.api.YouTubeVideoApi;
import com.athlete.atlas.infrastructure.api.model.YouTubeVideoCollection;
import com.athlete.atlas.input.rest.mapper.AthleteYouTubeVideoMapper;
import com.athlete.atlas.port.input.youtube.AthleteYouTubeVideoUseCase;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class YouTubeVideoController implements YouTubeVideoApi {

  private final AthleteYouTubeVideoUseCase athleteYouTubeVideoUseCase;

  private final AthleteYouTubeVideoMapper athleteYouTubeVideoMapper;

  @Override
  public ResponseEntity<YouTubeVideoCollection> listAthleteYouTubeVideos(
      @Nonnull final String athleteId) {
    final var selection = athleteYouTubeVideoUseCase.listAthleteYouTubeVideos(athleteId);

    return ResponseEntity.ok(athleteYouTubeVideoMapper.toDtoCollection(selection));
  }
}
