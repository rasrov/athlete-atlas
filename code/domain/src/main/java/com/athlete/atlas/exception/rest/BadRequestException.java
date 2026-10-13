package com.athlete.atlas.exception.rest;

import com.athlete.atlas.exception.DomainException;
import java.io.Serial;

public class BadRequestException extends DomainException {

  @Serial private static final long serialVersionUID = 3712940241439714501L;

  public BadRequestException(final String message) {
    super(message, RestClientErrorCode.BAD_REQUEST);
  }

  public BadRequestException(final String message, final Throwable cause) {
    super(message, RestClientErrorCode.BAD_REQUEST, cause);
  }

  public BadRequestException(final Throwable cause) {
    super(RestClientErrorCode.BAD_REQUEST, cause);
  }
}
