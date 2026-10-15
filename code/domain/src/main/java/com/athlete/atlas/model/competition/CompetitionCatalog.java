package com.athlete.atlas.model.competition;

import com.athlete.atlas.model.selection.SynchronizedSelection;
import jakarta.annotation.Nonnull;

/**
 * Competitions of a season obtained by the last successful refresh. It is shared by every athlete:
 * the season calendar does not depend on who participates.
 *
 * @param season range that was queried; a catalog of another season is not served.
 */
public record CompetitionCatalog(
    @Nonnull DateRange season, @Nonnull SynchronizedSelection<AthleteCompetition> competitions) {

  /** Keeps the competitions and their acquisition time; only records that the refresh failed. */
  public CompetitionCatalog withFailedRefresh() {
    return new CompetitionCatalog(season, competitions.withFailedRefresh());
  }
}
