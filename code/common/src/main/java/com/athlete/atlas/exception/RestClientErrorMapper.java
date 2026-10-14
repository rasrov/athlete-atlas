package com.athlete.atlas.exception;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
public class RestClientErrorMapper {

  private RestClientErrorMapper() {}

  /**
   * Maps Spring REST client (RestClient) exceptions to RemoteException, the domain exception
   * representing integration failures with upstreams.
   *
   * @param e Exception thrown by the REST client.
   * @param boundedContextCode Code of the bounded context to which the upstream belongs.
   * @return RemoteException corresponding to the integration failure.
   */
  public static RemoteException mapRemoteException(
      final RuntimeException e, final String boundedContextCode) {
    if (e instanceof final HttpStatusCodeException statusEx) {
      // The response body is never logged: provider error payloads may echo request data or
      // carry details that must not reach the logs. Adapters that need a diagnosis extract the
      // safe fields of their provider's error format themselves.
      log.warn("[{}] Upstream responded {}", boundedContextCode, statusEx.getStatusCode().value());
      return new RemoteException(
          String.format("[%s] upstream error", boundedContextCode),
          statusEx.getStatusCode().value(),
          e);
    }

    if (e instanceof final ResourceAccessException accessEx) {
      // Network failures: connection refused or connection/read timeouts (no HTTP response).
      final boolean isTimeout = isTimeout(accessEx);
      final int statusCode =
          isTimeout ? HttpStatus.GATEWAY_TIMEOUT.value() : HttpStatus.SERVICE_UNAVAILABLE.value();
      log.warn(
          "[{}] Upstream unreachable ({}): {}",
          boundedContextCode,
          isTimeout ? "timeout" : "connection error",
          accessEx.getMessage());
      return new RemoteException(
          String.format("[%s] upstream unreachable", boundedContextCode), statusCode, e);
    }

    // Any other REST client failure (e.g., deserialization errors): no valid response.
    log.warn("[{}] Unexpected rest client error: {}", boundedContextCode, e.getMessage());
    return new RemoteException(
        String.format("[%s] unexpected client error", boundedContextCode),
        HttpStatus.BAD_GATEWAY.value(),
        e);
  }

  /**
   * Handles a RemoteException by logging it and re-throwing it as a DomainException.
   *
   * @param e RemoteException to handle.
   * @param boundedContextCode Code of the bounded context to which the upstream belongs.
   * @return DomainException representing the integration failure.
   */
  public static DomainException handleRemoteException(
      final RemoteException e, final String boundedContextCode) {
    log.warn(
        "Error calling {} (status: {}). Propagating as remote error.",
        boundedContextCode,
        e.getStatusCode());
    throw e;
  }

  /**
   * Timeouts surface differently depending on the HTTP client: SocketTimeoutException (JDK
   * HttpURLConnection, Apache) or HttpTimeoutException (JDK HttpClient, the default one).
   */
  private static boolean isTimeout(final ResourceAccessException e) {
    for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
      if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
        return true;
      }
    }
    return false;
  }
}
