package com.athlete.atlas.input.rest.controller;

import com.athlete.atlas.infrastructure.api.HallOfFameApi;
import com.athlete.atlas.infrastructure.api.model.HallOfFameCollection;
import com.athlete.atlas.input.rest.ResponseCaching;
import com.athlete.atlas.input.rest.mapper.AthleteHallOfFameMapper;
import com.athlete.atlas.port.input.halloffame.AthleteHallOfFameUseCase;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HallOfFameController implements HallOfFameApi {

  private final AthleteHallOfFameUseCase athleteHallOfFameUseCase;

  private final AthleteHallOfFameMapper athleteHallOfFameMapper;

  @Override
  public ResponseEntity<HallOfFameCollection> listAthleteHallOfFame(
      @Nonnull final String athleteId) {
    final var selection = athleteHallOfFameUseCase.listAthleteHallOfFame(athleteId);

    return ResponseEntity.ok()
        .cacheControl(ResponseCaching.PUBLIC_DATA)
        .body(athleteHallOfFameMapper.toDtoCollection(selection));
  }
}
