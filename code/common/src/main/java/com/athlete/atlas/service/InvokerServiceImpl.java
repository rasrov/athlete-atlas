package com.athlete.atlas.service;

import com.athlete.atlas.exception.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public final class InvokerServiceImpl implements InvokerService {

  @Override
  public <T> T invoke(
      final Supplier<T> operation, final Function<RuntimeException, RemoteException> errorHandler) {
    try {
      return operation.get();
    } catch (final RestClientException e) {
      throw errorHandler.apply(e);
    }
  }
  @Override
  public <T> List<T> invokePaginated(
      final IntFunction<RemotePage<T>> operation, final int pageSize, final int maxPages,
      final Function<RuntimeException, RemoteException> errorHandler) {
    if (pageSize < 1 || maxPages < 1) {
      throw new IllegalArgumentException("Page size and maximum pages must be positive");
    }
    return invoke(() -> {
      final List<T> result = new ArrayList<>();
      Integer total = null;
      Integer pages = null;
      for (int pageNumber = 1; pageNumber <= maxPages; pageNumber++) {
        final RemotePage<T> page = operation.apply(pageNumber);
        if (page == null || page.items() == null || page.total() == null
            || page.totalPages() == null || page.total() < 0 || page.totalPages() < 0
            || page.totalPages() > maxPages) {
          throw new RestClientException("Invalid remote pagination metadata");
        }
        final long expectedPages = (page.total().longValue() + pageSize - 1) / pageSize;
        if (page.total() == 0 ? page.totalPages() > 1 : page.totalPages() != expectedPages) {
          throw new RestClientException("Inconsistent remote page count");
        }
        if (total != null && (!total.equals(page.total()) || !pages.equals(page.totalPages()))) {
          throw new RestClientException("Remote pagination changed while fetching");
        }
        total = page.total();
        pages = page.totalPages();
        final long expectedItems = Math.min(pageSize, total.longValue() - (long) (pageNumber - 1) * pageSize);
        if (page.items().size() != expectedItems || page.items().stream().anyMatch(java.util.Objects::isNull)) {
          throw new RestClientException("Incomplete remote page");
        }
        result.addAll(page.items());
        if (total == 0 || pageNumber == pages) {
          return List.copyOf(result);
        }
      }
      throw new RestClientException("Remote pagination exceeded the configured limit");
    }, errorHandler);
  }
}
