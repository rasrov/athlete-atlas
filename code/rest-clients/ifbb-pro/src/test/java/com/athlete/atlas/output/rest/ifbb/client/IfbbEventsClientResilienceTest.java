package com.athlete.atlas.output.rest.ifbb.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.output.rest.ifbb.generated.api.IfbbEventsApi;
import com.athlete.atlas.output.rest.ifbb.generated.model.EventPage;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

/** Mirrors the ifbbEventsApi resilience settings of boot's application.yml. */
@SpringBootTest(
    classes = IfbbEventsClientResilienceTest.TestApplication.class,
    properties = {
      "resilience4j.retry.instances.ifbbEventsApi.max-attempts=3",
      "resilience4j.retry.instances.ifbbEventsApi.wait-duration=1ms",
      "resilience4j.retry.instances.ifbbEventsApi.retry-exceptions="
          + "org.springframework.web.client.HttpServerErrorException,"
          + "org.springframework.web.client.HttpClientErrorException$TooManyRequests,"
          + "org.springframework.web.client.ResourceAccessException"
    })
class IfbbEventsClientResilienceTest {

  private static final DateRange SEASON = DateRange.seasonOf(LocalDate.of(2026, 10, 1));

  @SpringBootConfiguration
  @ImportAutoConfiguration({
    AopAutoConfiguration.class,
    io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration.class,
    io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration
        .class
  })
  @Import(IfbbEventsClient.class)
  static class TestApplication {}

  @MockitoBean private IfbbEventsApi ifbbEventsApi;

  @Autowired private IfbbEventsClient ifbbEventsClient;

  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  @AfterEach
  void resetCircuitBreaker() {
    circuitBreakerRegistry.circuitBreaker("ifbbEventsApi").reset();
  }

  @Test
  void retriesServerErrorsUntilSuccess() {
    when(ifbbEventsApi.listIfbbEvents(any(), any(), anyInt(), anyInt()))
        .thenThrow(httpError(HttpStatus.SERVICE_UNAVAILABLE))
        .thenReturn(new EventPage().events(List.of()).total(0).totalPages(0));

    assertThat(ifbbEventsClient.fetchPage(SEASON, 50, 1).total()).isZero();
    verify(ifbbEventsApi, times(2)).listIfbbEvents(any(), any(), anyInt(), anyInt());
  }

  @Test
  void retriesTooManyRequestsUpToMaxAttempts() {
    when(ifbbEventsApi.listIfbbEvents(any(), any(), anyInt(), anyInt()))
        .thenThrow(httpError(HttpStatus.TOO_MANY_REQUESTS));

    assertThatThrownBy(() -> ifbbEventsClient.fetchPage(SEASON, 50, 1))
        .isInstanceOf(HttpClientErrorException.TooManyRequests.class);
    verify(ifbbEventsApi, times(3)).listIfbbEvents(any(), any(), anyInt(), anyInt());
  }

  @Test
  void doesNotRetryOtherClientErrors() {
    when(ifbbEventsApi.listIfbbEvents(any(), any(), anyInt(), anyInt()))
        .thenThrow(httpError(HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> ifbbEventsClient.fetchPage(SEASON, 50, 1))
        .isInstanceOf(HttpClientErrorException.BadRequest.class);
    verify(ifbbEventsApi, times(1)).listIfbbEvents(any(), any(), anyInt(), anyInt());
  }

  @Test
  void openCircuitFailsFastAsServiceUnavailable() {
    circuitBreakerRegistry.circuitBreaker("ifbbEventsApi").transitionToOpenState();

    assertThatThrownBy(() -> ifbbEventsClient.fetchPage(SEASON, 50, 1))
        .isInstanceOfSatisfying(
            RemoteException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value()));
    verify(ifbbEventsApi, never()).listIfbbEvents(any(), any(), anyInt(), anyInt());
  }

  @Test
  void requestsTheRangeWithAnInclusiveEndDate() {
    when(ifbbEventsApi.listIfbbEvents(any(), any(), anyInt(), anyInt()))
        .thenReturn(new EventPage().events(List.of()).total(0).totalPages(0));

    ifbbEventsClient.fetchPage(SEASON, 50, 1);

    verify(ifbbEventsApi)
        .listIfbbEvents(LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30), 50, 1);
  }

  private static HttpStatusCodeException httpError(final HttpStatus status) {
    return status.is5xxServerError()
        ? HttpServerErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, null, null)
        : HttpClientErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, null, null);
  }
}
