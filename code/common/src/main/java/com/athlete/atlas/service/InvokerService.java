package com.athlete.atlas.service;

import com.athlete.atlas.exception.RemoteException;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface InvokerService {

  <T> T invoke(
      final Supplier<T> operation, final Function<RuntimeException, RemoteException> errorHandler);

  /** Fetches one-based pages sequentially; either returns every item or throws. */
  <T> List<T> invokePaginated(
      IntFunction<RemotePage<T>> operation, int pageSize, int maxPages,
      Function<RuntimeException, RemoteException> errorHandler);

  /**
   * Fetches token-paginated pages sequentially: the first page is requested with a null token and
   * each following page with the token returned by the previous one. Stops when the provider
   * returns no next token, when {@code isComplete} accepts the accumulated items (evaluated after
   * each page, so the operation can return already processed items) or after {@code maxPages}
   * pages; in all these cases the accumulated items are returned, even if fewer than wanted.
   * Invalid pages and repeated tokens are provider errors: they throw instead of returning a
   * partial result as if it were complete.
   */
  <T> List<T> invokeTokenPaginated(
      Function<String, TokenPage<T>> operation, Predicate<List<T>> isComplete, int maxPages,
      Function<RuntimeException, RemoteException> errorHandler);
}

