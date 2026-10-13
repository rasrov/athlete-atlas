package com.athlete.atlas.input.rest.controller;

import com.athlete.atlas.infrastructure.api.CompetitionApi;
import com.athlete.atlas.infrastructure.api.model.CompetitionCollection;
import com.athlete.atlas.infrastructure.api.model.ParticipationStatus;
import com.athlete.atlas.input.rest.mapper.AthleteCompetitionMapper;
import com.athlete.atlas.port.input.competition.AthleteCompetitionUseCase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
      @Nonnull final String athleteId, @Nullable final ParticipationStatus participationStatus) {
    final var status = athleteCompetitionMapper.toDomain(participationStatus);
    final var athleteCompetitions =
        athleteCompetitionUseCase.listAthleteCompetitions(athleteId, status);

    return ResponseEntity.ok(
        athleteCompetitionMapper.toDtoCollection(athleteId, athleteCompetitions));
  }
}
