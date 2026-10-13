package com.athlete.atlas.service;

import com.athlete.atlas.exception.RemoteException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Categorizes failures occurring during the execution of tasks against external systems.
 *
 * <p>A failure is considered <b>technical</b> (an actual degradation: upstream down, global
 * timeout, or rejection due to saturation) as opposed to a <b>legitimate absence</b> such as a 404
 * ({@link com.athlete.atlas.exception.NotFoundException}), which does not represent a system
 * failure. This distinction allows use cases to determine their degradation policy without
 * duplicating logic.
 */
public final class FailureClassifier {

  private FailureClassifier() {}

  public static boolean isTechnicalFailure(final Throwable error) {
    return error instanceof RemoteException
        || error instanceof TimeoutException
        || error instanceof RejectedExecutionException;
  }
}
