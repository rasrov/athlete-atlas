package com.athlete.atlas.input.rest.controller;

import com.athlete.atlas.infrastructure.api.CompetitionParticipationApi;
import com.athlete.atlas.infrastructure.api.model.ParticipationUpdate;
import com.athlete.atlas.input.rest.ResponseCaching;
import com.athlete.atlas.port.input.competition.SetCompetitionParticipationUseCase;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Write endpoint, registered only with api.write-enabled=true: without authentication anyone could
 * change the participations, so it stays disabled in public environments (its route answers 404).
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "api.write-enabled", havingValue = "true")
public class CompetitionParticipationController implements CompetitionParticipationApi {

  private final SetCompetitionParticipationUseCase setCompetitionParticipationUseCase;

  @Override
  public ResponseEntity<Void> setAthleteCompetitionParticipation(
      @Nonnull final String athleteId,
      @Nonnull final String competitionId,
      @Nonnull final ParticipationUpdate participationUpdate) {
    setCompetitionParticipationUseCase.setParticipation(
        athleteId, competitionId, participationUpdate.getConfirmed());

    return ResponseEntity.noContent().cacheControl(ResponseCaching.NO_STORE).build();
  }
}
