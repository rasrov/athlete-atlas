package com.athlete.atlas.input.rest.controller;

import com.athlete.atlas.infrastructure.api.CompetitionApi;
import com.athlete.atlas.infrastructure.api.model.CompetitionCollection;
import com.athlete.atlas.input.rest.mapper.AthleteCompetitionMapper;
import com.athlete.atlas.port.input.competition.AthleteCompetitionUseCase;
import com.athlete.atlas.input.rest.ResponseCaching;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CompetitionController implements CompetitionApi {

  private final AthleteCompetitionUseCase athleteCompetitionUseCase;

  private final AthleteCompetitionMapper athleteCompetitionMapper;

  @Override
  public ResponseEntity<CompetitionCollection> listAthleteCompetitions(
      @Nonnull final String athleteId) {
    final var selection = athleteCompetitionUseCase.listAthleteCompetitions(athleteId);

    return ResponseEntity.ok()
        .cacheControl(ResponseCaching.forAvailability(selection.availability()))
        .body(athleteCompetitionMapper.toDtoCollection(selection));
  }
}
