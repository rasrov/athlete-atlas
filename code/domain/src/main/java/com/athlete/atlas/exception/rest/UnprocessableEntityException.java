package com.athlete.atlas.exception.rest;

import com.athlete.atlas.exception.DomainException;
import java.io.Serial;
import lombok.Getter;

@Getter
public class UnprocessableEntityException extends DomainException {

  @Serial private static final long serialVersionUID = 3712940241439714502L;

  public UnprocessableEntityException(final String message) {
    super(message, RestClientErrorCode.UNPROCESSABLE_ENTITY);
  }

  public UnprocessableEntityException(final String message, final Throwable cause) {
    super(message, RestClientErrorCode.UNPROCESSABLE_ENTITY, cause);
  }

  public UnprocessableEntityException(final Throwable cause) {
    super(RestClientErrorCode.UNPROCESSABLE_ENTITY, cause);
  }
}
