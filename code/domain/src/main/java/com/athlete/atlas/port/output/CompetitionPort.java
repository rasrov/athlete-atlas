package com.athlete.atlas.port.output;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.DateRange;
import jakarta.annotation.Nonnull;
import java.util.List;

public interface CompetitionPort {

  /**
   * Fetches the competitions that overlap the given range, sorted by start date.
   *
   * @param range half-open date range to query.
   * @return valid competitions; invalid or duplicate provider entries are discarded.
   */
  List<AthleteCompetition> fetchCompetitions(@Nonnull DateRange range);
}
