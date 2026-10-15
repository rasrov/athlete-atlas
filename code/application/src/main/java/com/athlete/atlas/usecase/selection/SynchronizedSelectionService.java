package com.athlete.atlas.usecase.selection;

import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.athlete.Athlete;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.port.output.AthleteRepository;
import com.athlete.atlas.port.output.SelectionRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Serves the stored selection of each athlete and refreshes it from a provider (e.g. YouTube,
 * Instagram). The latest successful refresh always wins, whatever the number of items (fewer than
 * the selection size, even none, is valid data). A failed refresh keeps the previous selection and
 * marks it stale; without any successful refresh there is nothing to serve.
 *
 * <p>The provider account of each athlete comes from the athlete storage. If the storage does not
 * respond during a refresh, the previous selections are kept and marked stale.
 *
 * @param <T> type of the selected items.
 */
@Slf4j
public abstract class SynchronizedSelectionService<T> {

  private final String source;

  private final SelectionRepository<T> selectionRepository;

  private final AthleteRepository athleteRepository;

  private final Clock clock;

  /** Athletes of the last refresh, whose selections are marked stale if the storage fails. */
  private volatile Set<String> refreshedAthletes = Set.of();

  /**
   * @param source name of the provider, used in logs and messages.
   */
  protected SynchronizedSelectionService(
      final String source,
      final SelectionRepository<T> selectionRepository,
      final AthleteRepository athleteRepository,
      final Clock clock) {
    this.source = source;
    this.selectionRepository = selectionRepository;
    this.athleteRepository = athleteRepository;
    this.clock = clock;
  }

  /** Provider account of the athlete (e.g. YouTube channel id); null if the athlete has none. */
  @Nullable
  protected abstract String account(Athlete athlete);

  /** Age after which a selection is stale even if its last refresh succeeded. */
  protected abstract Duration freshnessThreshold();

  /**
   * Fetches the current selection of a provider account.
   *
   * @throws RemoteException when the provider fails.
   */
  protected abstract List<T> fetch(String account);

  /**
   * Returns the stored selection of the athlete; it does not call the provider.
   *
   * @throws NotFoundException if the athlete does not exist or has no account for this provider.
   * @throws DataUnavailableException if no refresh has succeeded yet.
   * @throws StorageUnavailableException if the athlete storage does not respond.
   */
  protected AthleteSelection<T> getSelection(@Nonnull final String athleteId) {
    if (athleteRepository.findByCode(athleteId).map(this::account).isEmpty()) {
      throw new NotFoundException("Athlete without " + source + " account: " + athleteId);
    }
    final var selection =
        selectionRepository
            .findByAthleteId(athleteId)
            .orElseThrow(
                () ->
                    new DataUnavailableException(
                        source + " content not synchronized yet for athlete " + athleteId));
    return new AthleteSelection<>(
        athleteId,
        selection.items(),
        selection.updatedAt(),
        selection.availability(clock.instant(), freshnessThreshold()));
  }

  /**
   * Refreshes the selection of every athlete with an account for this provider. A failure of one
   * athlete does not stop the others, and never discards a previous selection.
   */
  public void refreshAll() {
    final Map<String, String> accounts;
    try {
      accounts = accounts();
    } catch (final RuntimeException e) {
      refreshedAthletes.forEach(this::markFailed);
      if (e instanceof StorageUnavailableException) {
        log.warn(
            "Athletes unavailable, skipping the {} refresh and keeping the previous selections: {}",
            source,
            e.getMessage());
      } else {
        log.error(
            "Unexpected error reading the athletes, skipping the {} refresh and keeping the"
                + " previous selections",
            source,
            e);
      }
      return;
    }
    refreshedAthletes = Set.copyOf(accounts.keySet());
    accounts.forEach(this::refresh);
  }

  /** Provider account of each athlete that has one, keyed by athlete id. */
  private Map<String, String> accounts() {
    final var accounts = new LinkedHashMap<String, String>();
    for (final var athlete : athleteRepository.findAll()) {
      final var account = account(athlete);
      if (account != null) {
        accounts.put(athlete.code(), account);
      }
    }
    return accounts;
  }

  private void refresh(final String athleteId, final String account) {
    try {
      final var items = fetch(account);
      selectionRepository.save(athleteId, SynchronizedSelection.refreshedAt(items, clock.instant()));
      log.info("{} selection of athlete {} refreshed: {} items", source, athleteId, items.size());
    } catch (final RuntimeException e) {
      markFailed(athleteId);
      if (e instanceof RemoteException) {
        // The provider error is already logged where it is translated.
        log.warn(
            "{} refresh of athlete {} failed, keeping the previous selection: {}",
            source,
            athleteId,
            e.getMessage());
      } else {
        log.error(
            "Unexpected error refreshing the {} selection of athlete {}, keeping the previous"
                + " selection",
            source,
            athleteId,
            e);
      }
    }
  }

  private void markFailed(final String athleteId) {
    selectionRepository
        .findByAthleteId(athleteId)
        .ifPresent(previous -> selectionRepository.save(athleteId, previous.withFailedRefresh()));
  }
}
