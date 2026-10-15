package com.athlete.atlas.port.input.competition;

public interface RefreshCompetitionsUseCase {

  /**
   * Refreshes the competition catalog of the current season. A failure never discards the previous
   * catalog.
   */
  void refresh();
}
