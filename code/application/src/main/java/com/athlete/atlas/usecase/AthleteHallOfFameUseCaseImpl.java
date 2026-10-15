package com.athlete.atlas.usecase;

import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.halloffame.HallOfFameEntry;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.port.input.halloffame.AthleteHallOfFameUseCase;
import com.athlete.atlas.port.output.AthleteRepository;
import com.athlete.atlas.port.output.HallOfFameRepository;
import jakarta.annotation.Nonnull;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Serves the competition results of the athlete from the storage. They are not synchronized with a
 * provider, so there is no stale state: if the storage does not respond, the request fails.
 */
@Component
@RequiredArgsConstructor
public class AthleteHallOfFameUseCaseImpl implements AthleteHallOfFameUseCase {

  private final AthleteRepository athleteRepository;

  private final HallOfFameRepository hallOfFameRepository;

  @Override
  public AthleteSelection<HallOfFameEntry> listAthleteHallOfFame(@Nonnull final String athleteId) {
    if (athleteRepository.findByCode(athleteId).isEmpty()) {
      throw new NotFoundException("Athlete not found: " + athleteId);
    }
    final var entries = hallOfFameRepository.findByAthleteCode(athleteId);
    final var updatedAt =
        entries.stream().map(HallOfFameEntry::updatedAt).max(Comparator.naturalOrder()).orElse(null);
    return new AthleteSelection<>(athleteId, entries, updatedAt, Availability.AVAILABLE);
  }
}
