package com.athlete.atlas.exception;

import java.io.Serial;

public class NotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 4184660980729738698L;

  public NotFoundException(final String message) {
    super(message);
  }

  public NotFoundException(final Throwable cause) {
    super(cause);
  }

  public NotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
