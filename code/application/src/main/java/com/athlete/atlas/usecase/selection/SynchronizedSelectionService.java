package com.athlete.atlas.usecase.selection;

import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.model.selection.SynchronizedSelection;
import com.athlete.atlas.port.output.SelectionRepository;
import jakarta.annotation.Nonnull;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Serves the stored selection of each athlete and refreshes it from a provider (e.g. YouTube,
 * Instagram). The latest successful refresh always wins, whatever the number of items (fewer than
 * the selection size, even none, is valid data). A failed refresh keeps the previous selection and
 * marks it stale; without any successful refresh there is nothing to serve.
 *
 * @param <T> type of the selected items.
 */
@Slf4j
public abstract class SynchronizedSelectionService<T> {

  private final String source;

  private final SelectionRepository<T> selectionRepository;

  private final Clock clock;

  /**
   * @param source name of the provider, used in logs and messages.
   */
  protected SynchronizedSelectionService(
      final String source, final SelectionRepository<T> selectionRepository, final Clock clock) {
    this.source = source;
    this.selectionRepository = selectionRepository;
    this.clock = clock;
  }

  /** Provider account of each athlete (e.g. YouTube channel id), keyed by athlete id. */
  protected abstract Map<String, String> accounts();

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
   * @throws NotFoundException if the athlete has no account for this provider.
   * @throws DataUnavailableException if no refresh has succeeded yet.
   */
  protected AthleteSelection<T> getSelection(@Nonnull final String athleteId) {
    if (!accounts().containsKey(athleteId)) {
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
   * Refreshes the selection of every configured athlete. A failure of one athlete does not stop the
   * others, and never discards a previous selection.
   */
  public void refreshAll() {
    accounts().forEach(this::refresh);
  }

  private void refresh(final String athleteId, final String account) {
    try {
      final var items = fetch(account);
      selectionRepository.save(athleteId, SynchronizedSelection.refreshedAt(items, clock.instant()));
      log.info("{} selection of athlete {} refreshed: {} items", source, athleteId, items.size());
    } catch (final RuntimeException e) {
      selectionRepository
          .findByAthleteId(athleteId)
          .ifPresent(previous -> selectionRepository.save(athleteId, previous.withFailedRefresh()));
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
}
