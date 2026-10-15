package com.athlete.atlas.input.rest.controller;

import com.athlete.atlas.infrastructure.api.InstagramPostApi;
import com.athlete.atlas.infrastructure.api.model.InstagramPostCollection;
import com.athlete.atlas.input.rest.mapper.AthleteInstagramPostMapper;
import com.athlete.atlas.port.input.instagram.AthleteInstagramPostUseCase;
import com.athlete.atlas.input.rest.ResponseCaching;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstagramPostController implements InstagramPostApi {

  private final AthleteInstagramPostUseCase athleteInstagramPostUseCase;

  private final AthleteInstagramPostMapper athleteInstagramPostMapper;

  @Override
  public ResponseEntity<InstagramPostCollection> listAthleteInstagramPosts(
      @Nonnull final String athleteId) {
    final var selection = athleteInstagramPostUseCase.listAthleteInstagramPosts(athleteId);

    return ResponseEntity.ok()
        .cacheControl(ResponseCaching.PUBLIC_DATA)
        .body(athleteInstagramPostMapper.toDtoCollection(selection));
  }
}
