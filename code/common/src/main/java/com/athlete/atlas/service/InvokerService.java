package com.athlete.atlas.service;

import com.athlete.atlas.exception.RemoteException;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface InvokerService {

  <T> T invoke(
      final Supplier<T> operation, final Function<RuntimeException, RemoteException> errorHandler);

  /** Fetches one-based pages sequentially; either returns every item or throws. */
  <T> List<T> invokePaginated(
      IntFunction<RemotePage<T>> operation, int pageSize, int maxPages,
      Function<RuntimeException, RemoteException> errorHandler);
}

