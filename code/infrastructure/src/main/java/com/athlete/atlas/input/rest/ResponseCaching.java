package com.athlete.atlas.input.rest;

import com.athlete.atlas.model.enums.Availability;
import java.time.Duration;
import org.springframework.http.CacheControl;

/**
 * Cache-Control policies of the API. The data is public, identical for every client and refreshed
 * every few hours, so successful responses can be reused by browsers and by a CDN in front of the
 * backend, which then absorbs most of the traffic. Errors are never stored, so that a transient 503
 * is not served once the data is available.
 */
public final class ResponseCaching {

  /**
   * Reusable for 5 minutes; afterwards a cache may keep serving it for 1 minute while it
   * revalidates, or for 1 hour while the backend answers with errors.
   */
  public static final CacheControl PUBLIC_DATA =
      CacheControl.maxAge(Duration.ofMinutes(5))
          .cachePublic()
          .staleWhileRevalidate(Duration.ofMinutes(1))
          .staleIfError(Duration.ofHours(1));

  /**
   * Never stored by any cache. Used for errors, degraded responses and the responses of write
   * endpoints.
   */
  public static final CacheControl NO_STORE = CacheControl.noStore();

  /**
   * Data of unknown availability is a degraded response (e.g. the storage did not respond), so it is
   * not stored either: it would keep being served after the backend recovers.
   */
  public static CacheControl forAvailability(final Availability availability) {
    return availability == Availability.UNKNOWN ? NO_STORE : PUBLIC_DATA;
  }

  private ResponseCaching() {}
}
