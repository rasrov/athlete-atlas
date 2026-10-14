package com.athlete.atlas.model.enums;

/** Freshness of synchronized data, as exposed in the API metadata. */
public enum Availability {
  /** The last refresh succeeded within the freshness threshold. */
  AVAILABLE,

  /** The last refresh failed or the data is older than the freshness threshold. */
  STALE,

  /** The age of the data is unknown. */
  UNKNOWN
}
