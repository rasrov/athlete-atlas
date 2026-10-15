package com.athlete.atlas.output.persistence.jpa;

/**
 * Caffeine caches of the data owned by the backend, keyed by athlete code. They are configured in
 * spring.cache of application.yml and evicted when the data is written.
 */
public final class CacheNames {

  public static final String ATHLETES = "athletes";

  public static final String CONFIRMED_COMPETITIONS = "confirmedCompetitions";

  public static final String HALL_OF_FAME = "hallOfFame";

  private CacheNames() {}
}
