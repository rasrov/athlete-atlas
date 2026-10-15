package com.athlete.atlas.output.persistence;

import com.athlete.atlas.exception.StorageUnavailableException;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * Translates the failures of an unavailable database (connection refused, pool exhausted, timeout)
 * into {@link StorageUnavailableException}, so that the application can degrade without depending
 * on Spring Data. Any other error (e.g. a wrong query) is a bug and propagates as is.
 */
public final class StorageCalls {

  private StorageCalls() {}

  public static <T> T call(final Supplier<T> query) {
    try {
      return query.get();
    } catch (final DataAccessResourceFailureException
        | TransientDataAccessException
        | RecoverableDataAccessException
        | CannotCreateTransactionException e) {
      throw new StorageUnavailableException(
          "Database unavailable: " + e.getClass().getSimpleName(), e);
    }
  }
}
