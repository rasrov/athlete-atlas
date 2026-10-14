package com.athlete.atlas.exception;

import java.io.Serial;

/** The requested data has not been synchronized yet, so there is nothing to serve. */
public class DataUnavailableException extends RuntimeException {

  @Serial private static final long serialVersionUID = 6250930711250318427L;

  public DataUnavailableException(final String message) {
    super(message);
  }
}
