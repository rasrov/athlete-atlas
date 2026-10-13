package com.athlete.atlas.output.rest.ifbb.client;

import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.service.RemotePage;
import com.athlete.atlas.output.rest.ifbb.generated.api.IfbbEventsApi;
import com.athlete.atlas.output.rest.ifbb.generated.model.Event;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Transport layer for the IFBB events API. Resilience is applied per page so that a transient
 * failure only repeats the failed request, not the whole pagination. Which errors are retried is
 * configured under {@code resilience4j.*.instances.ifbbEventsApi}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IfbbEventsClient {

  public static final String BOUNDED_CONTEXT = "ifbb_events_api";

  private static final String RESILIENCE_INSTANCE = "ifbbEventsApi";

  private final IfbbEventsApi ifbbEventsApi;

  /**
   * Fetches one one-based page of events. Spring REST client exceptions are propagated untouched
   * so that the caller can translate them.
   *
   * @return the page, or {@code null} when the provider returns an empty body.
   */
  @Retry(name = RESILIENCE_INSTANCE)
  @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "circuitOpenFallback")
  public RemotePage<Event> fetchPage(final DateRange range, final int pageSize, final int page) {
    final var response =
        ifbbEventsApi.listIfbbEvents(range.start(), range.endInclusive(), pageSize, page);
    return response == null
        ? null
        : new RemotePage<>(response.getEvents(), response.getTotal(), response.getTotalPages());
  }

  /** Only handles an open circuit; any other failure reaches the caller unchanged. */
  private RemotePage<Event> circuitOpenFallback(
      final DateRange range, final int pageSize, final int page, final CallNotPermittedException e) {
    log.warn("[{}] Circuit breaker open, page {} not requested", BOUNDED_CONTEXT, page);
    throw new RemoteException(
        String.format("[%s] circuit breaker open", BOUNDED_CONTEXT),
        HttpStatus.SERVICE_UNAVAILABLE.value(),
        e);
  }
}
