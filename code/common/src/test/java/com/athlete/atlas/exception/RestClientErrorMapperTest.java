package com.athlete.atlas.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@ExtendWith(OutputCaptureExtension.class)
class RestClientErrorMapperTest {

  private static final String CONTEXT = "test_api";

  @Test
  void mapsHttpErrorsKeepingTheStatusWithoutLoggingTheBody(final CapturedOutput output) {
    final var body = "{\"error\": {\"message\": \"SENSITIVE-BODY-CONTENT\"}}";
    final var error =
        HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            HttpHeaders.EMPTY,
            body.getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);

    final var mapped = RestClientErrorMapper.mapRemoteException(error, CONTEXT);

    assertThat(mapped.getStatusCode()).isEqualTo(401);
    assertThat(mapped.getMessage()).isEqualTo("[test_api] upstream error");
    assertThat(output.getAll()).contains("[test_api] Upstream responded 401");
    assertThat(output.getAll()).doesNotContain("SENSITIVE-BODY-CONTENT");
  }

  @Test
  void mapsTimeoutsToGatewayTimeoutAndConnectionErrorsToServiceUnavailable() {
    final var socketTimeout =
        new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out"));
    final var httpClientTimeout =
        new ResourceAccessException("I/O error", new HttpTimeoutException("request timed out"));
    final var refused = new ResourceAccessException("Connection refused");

    assertThat(RestClientErrorMapper.mapRemoteException(socketTimeout, CONTEXT).getStatusCode())
        .isEqualTo(504);
    assertThat(RestClientErrorMapper.mapRemoteException(httpClientTimeout, CONTEXT).getStatusCode())
        .isEqualTo(504);
    assertThat(RestClientErrorMapper.mapRemoteException(refused, CONTEXT).getStatusCode())
        .isEqualTo(503);
  }

  @Test
  void mapsOtherClientErrorsToBadGateway() {
    assertThat(
            RestClientErrorMapper.mapRemoteException(new RestClientException("bad"), CONTEXT)
                .getStatusCode())
        .isEqualTo(502);
  }
}
