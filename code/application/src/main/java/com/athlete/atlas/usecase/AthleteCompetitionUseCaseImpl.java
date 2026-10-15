package com.athlete.atlas.usecase;

import com.athlete.atlas.config.CompetitionCatalogProperties;
import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.CompetitionCatalog;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.port.input.competition.AthleteCompetitionUseCase;
import com.athlete.atlas.port.input.competition.RefreshCompetitionsUseCase;
import com.athlete.atlas.port.output.AthleteRepository;
import com.athlete.atlas.port.output.CompetitionCatalogRepository;
import com.athlete.atlas.port.output.CompetitionParticipationRepository;
import com.athlete.atlas.port.output.CompetitionPort;
import jakarta.annotation.Nonnull;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Serves the stored competition catalog of the current season and refreshes it from the provider.
 * The latest successful refresh always wins; a failed refresh keeps the previous catalog and marks
 * it stale. Until a refresh of the current season succeeds (on startup, or when a new season
 * begins) there is nothing to serve.
 *
 * <p>Only the competitions confirmed by the athlete are served; the confirmations come from the
 * storage, which only returns the confirmed ones. If the storage does not respond, an empty
 * calendar of unknown availability is served instead of an error.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AthleteCompetitionUseCaseImpl
    implements AthleteCompetitionUseCase, RefreshCompetitionsUseCase {

  private final CompetitionPort competitionPort;

  private final CompetitionCatalogRepository competitionCatalogRepository;

  private final CompetitionCatalogProperties competitionCatalogProperties;

  private final AthleteRepository athleteRepository;

  private final CompetitionParticipationRepository competitionParticipationRepository;

  private final Clock clock;

  @Override
  public AthleteSelection<AthleteCompetition> listAthleteCompetitions(
      @Nonnull final String athleteId) {
    final Set<String> confirmedIds;
    try {
      if (athleteRepository.findByCode(athleteId).isEmpty()) {
        throw new NotFoundException("Athlete not found: " + athleteId);
      }
      confirmedIds = competitionParticipationRepository.findConfirmedCompetitionIds(athleteId);
    } catch (final StorageUnavailableException e) {
      log.warn(
          "Participations of athlete {} unavailable, serving an empty calendar: {}",
          athleteId,
          e.getMessage());
      return new AthleteSelection<>(athleteId, List.of(), null, Availability.UNKNOWN);
    }

    final var season = currentSeason();
    final var competitions =
        competitionCatalogRepository
            .find()
            .filter(catalog -> catalog.season().equals(season))
            .map(CompetitionCatalog::competitions)
            .orElseThrow(
                () ->
                    new DataUnavailableException(
                        "Competitions of season " + season.start() + " not synchronized yet"));
    final var items =
        competitions.items().stream()
            .filter(competition -> confirmedIds.contains(competition.id()))
            .toList();
    return new AthleteSelection<>(
        athleteId,
        items,
        competitions.updatedAt(),
        competitions.availability(
            clock.instant(), competitionCatalogProperties.freshnessThreshold()));
  }

  @Override
  public void refresh() {
    final var season = currentSeason();
    try {
      final var competitions = competitionPort.fetchCompetitions(season);
      competitionCatalogRepository.save(
          new CompetitionCatalog(
              season, SynchronizedSelection.refreshedAt(competitions, clock.instant())));
      log.info(
          "Competitions of season {} refreshed: {} competitions",
          season.start(),
          competitions.size());
    } catch (final RuntimeException e) {
      competitionCatalogRepository
          .find()
          .ifPresent(previous -> competitionCatalogRepository.save(previous.withFailedRefresh()));
      if (e instanceof RemoteException) {
        // The provider error is already logged where it is translated.
        log.warn(
            "Refresh of the competitions of season {} failed, keeping the previous catalog: {}",
            season.start(),
            e.getMessage());
      } else {
        log.error(
            "Unexpected error refreshing the competitions of season {}, keeping the previous"
                + " catalog",
            season.start(),
            e);
      }
    }
  }

  private DateRange currentSeason() {
    return DateRange.seasonOf(LocalDate.now(clock));
  }
}
