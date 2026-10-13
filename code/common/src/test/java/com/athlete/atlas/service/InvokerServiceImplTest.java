package com.athlete.atlas.service;

import static org.junit.jupiter.api.Assertions.*;

import com.athlete.atlas.exception.RemoteException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class InvokerServiceImplTest {

  private final InvokerService service = new InvokerServiceImpl();

  private RemoteException error(RuntimeException e) {
    return new RemoteException("upstream", 502, e);
  }

  @Test
  void fetchesAllPagesInOrder() {
    var calls = new ArrayList<Integer>();
    var result =
        service.invokePaginated(
            page -> {
              calls.add(page);
              return new RemotePage<>(page == 1 ? List.of(1, 2) : List.of(3), 3, 2);
            },
            2,
            10,
            this::error);
    assertEquals(List.of(1, 2, 3), result);
    assertEquals(List.of(1, 2), calls);
  }

  @Test
  void acceptsEmptyCatalogWithZeroOrOnePages() {
    for (int pages : List.of(0, 1)) {
      assertTrue(
          service
              .invokePaginated(p -> new RemotePage<>(List.of(), 0, pages), 50, 200, this::error)
              .isEmpty());
    }
  }

  @Test
  void rejectsInvalidAndIncompletePages() {
    var invalid =
        List.of(
            new RemotePage<>(List.of(1), 2, 1),
            new RemotePage<>(List.of(1), 1, 201),
            new RemotePage<>(List.of(1), 1, 0),
            new RemotePage<Integer>(null, 1, 1),
            new RemotePage<>(List.of(1), null, 1));
    for (var page : invalid) {
      assertThrows(
          RemoteException.class, () -> service.invokePaginated(p -> page, 2, 200, this::error));
    }
    assertThrows(
        RemoteException.class, () -> service.invokePaginated(p -> null, 2, 200, this::error));
  }

  @Test
  void rejectsChangingTotals() {
    assertThrows(
        RemoteException.class,
        () ->
            service.invokePaginated(
                p -> new RemotePage<>(List.of(1, 2), p == 1 ? 4 : 6, p == 1 ? 2 : 3),
                2,
                10,
                this::error));
  }

  @Test
  void mapsLaterPageFailureWithoutReturningPartialData() {
    assertThrows(
        RemoteException.class,
        () ->
            service.invokePaginated(
                p -> {
                  if (p == 2) throw new RestClientException("failed");
                  return new RemotePage<>(List.of(1, 2), 3, 2);
                },
                2,
                10,
                this::error));
  }

  @Test
  void rejectsInvalidConfigurationBeforeCallingProvider() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.invokePaginated(p -> fail(), 0, 200, this::error));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.invokePaginated(p -> fail(), 50, 0, this::error));
  }
}
