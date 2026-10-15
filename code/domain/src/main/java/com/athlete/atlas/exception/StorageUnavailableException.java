package com.athlete.atlas.exception;

import java.io.Serial;

/** The storage of the backend (e.g. the database) does not respond. */
public class StorageUnavailableException extends RuntimeException {

  @Serial private static final long serialVersionUID = -3412470158722384765L;

  public StorageUnavailableException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
