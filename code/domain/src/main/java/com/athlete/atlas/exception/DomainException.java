package com.athlete.atlas.exception;

import com.athlete.atlas.exception.rest.ErrorCode;
import jakarta.annotation.Nullable;
import java.io.Serial;
import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

  @Serial private static final long serialVersionUID = 8871478733470211588L;

  private final ErrorCode errorCode;

  public DomainException(@Nullable String message) {
    super(message);
    this.errorCode = null;
  }

  public DomainException(@Nullable Throwable cause) {
    super(cause);
    this.errorCode = null;
  }

  public DomainException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
    this.errorCode = null;
  }

  public DomainException(final String message, final ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public DomainException(final ErrorCode errorCode, final Throwable cause) {
    super(errorCode.message(), cause);
    this.errorCode = errorCode;
  }

  public DomainException(final String message, final ErrorCode errorCode, final Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
