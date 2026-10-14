package com.athlete.atlas.service;

import static org.junit.jupiter.api.Assertions.*;

import com.athlete.atlas.exception.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
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

  @Test
  void tokenPaginationStartsWithoutTokenAndFollowsTheReturnedTokens() {
    var tokens = new ArrayList<String>();
    var result =
        service.invokeTokenPaginated(
            token -> {
              tokens.add(token);
              return token == null
                  ? new TokenPage<>(List.of(1), "a")
                  : token.equals("a") ? new TokenPage<>(List.of(2), "b") : new TokenPage<>(List.of(3), null);
            },
            items -> false,
            10,
            this::error);
    assertEquals(List.of(1, 2, 3), result);
    assertEquals(Arrays.asList(null, "a", "b"), tokens);
  }

  @Test
  void tokenPaginationStopsWhenTheAccumulatedItemsAreComplete() {
    var calls = new ArrayList<String>();
    var result =
        service.invokeTokenPaginated(
            token -> {
              calls.add(token);
              return new TokenPage<>(List.of(calls.size()), "t" + calls.size());
            },
            items -> items.size() == 2,
            10,
            this::error);
    assertEquals(List.of(1, 2), result);
    assertEquals(2, calls.size());
  }

  @Test
  void tokenPaginationReturnsWhatWasFoundWhenThereAreNoMorePages() {
    for (var lastToken : Arrays.asList(null, "", " ")) {
      assertEquals(
          List.of(1),
          service.invokeTokenPaginated(
              token -> new TokenPage<>(List.of(1), lastToken), items -> false, 10, this::error));
    }
  }

  @Test
  void tokenPaginationReturnsWhatWasFoundAtThePageLimit() {
    var calls = new ArrayList<String>();
    var result =
        service.invokeTokenPaginated(
            token -> {
              calls.add(token);
              return new TokenPage<>(List.of(calls.size()), "t" + calls.size());
            },
            items -> false,
            4,
            this::error);
    assertEquals(List.of(1, 2, 3, 4), result);
    assertEquals(4, calls.size());
  }

  @Test
  void tokenPaginationRejectsRepeatedTokens() {
    var calls = new ArrayList<String>();
    assertThrows(
        RemoteException.class,
        () ->
            service.invokeTokenPaginated(
                token -> {
                  calls.add(token);
                  return new TokenPage<>(List.of(1), calls.size() == 1 ? "a" : calls.size() == 2 ? "b" : "a");
                },
                items -> false,
                10,
                this::error));
    assertEquals(3, calls.size());
  }

  @Test
  void tokenPaginationRejectsInvalidPages() {
    var invalid =
        Arrays.asList(
            null, new TokenPage<Integer>(null, null), new TokenPage<>(Arrays.asList(1, null), null));
    for (var page : invalid) {
      assertThrows(
          RemoteException.class,
          () -> service.invokeTokenPaginated(token -> page, items -> false, 4, this::error));
    }
  }

  @Test
  void tokenPaginationMapsLaterPageFailureWithoutReturningPartialData() {
    assertThrows(
        RemoteException.class,
        () ->
            service.invokeTokenPaginated(
                token -> {
                  if (token != null) throw new RestClientException("failed");
                  return new TokenPage<>(List.of(1), "a");
                },
                items -> false,
                4,
                this::error));
  }

  @Test
  void tokenPaginationRejectsInvalidConfigurationBeforeCallingProvider() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.invokeTokenPaginated(token -> fail(), items -> false, 0, this::error));
  }
}
