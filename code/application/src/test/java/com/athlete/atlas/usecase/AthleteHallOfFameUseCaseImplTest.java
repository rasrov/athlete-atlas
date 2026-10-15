package com.athlete.atlas.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.athlete.Athlete;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.halloffame.HallOfFameEntry;
import com.athlete.atlas.port.output.AthleteRepository;
import com.athlete.atlas.port.output.HallOfFameRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AthleteHallOfFameUseCaseImplTest {

  private static final String ATHLETE = "kim-angel";

  private AthleteRepository athleteRepository;

  private HallOfFameRepository hallOfFameRepository;

  private AthleteHallOfFameUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    athleteRepository = mock(AthleteRepository.class);
    hallOfFameRepository = mock(HallOfFameRepository.class);
    useCase = new AthleteHallOfFameUseCaseImpl(athleteRepository, hallOfFameRepository);
    when(athleteRepository.findByCode(ATHLETE))
        .thenReturn(Optional.of(new Athlete(ATHLETE, null, null)));
  }

  @Test
  void anUnknownAthleteIsNotFound() {
    assertThatThrownBy(() -> useCase.listAthleteHallOfFame("unknown"))
        .isInstanceOf(NotFoundException.class);
    verifyNoInteractions(hallOfFameRepository);
  }

  @Test
  void servesTheResultsInTheStorageOrderWithTheirLastModification() {
    final var latest = entry("1", "2026-09-12", "2026-10-01T10:00:00Z");
    final var older = entry("2", "2025-11-08", "2026-10-15T18:00:00Z");
    when(hallOfFameRepository.findByAthleteCode(ATHLETE)).thenReturn(List.of(latest, older));

    final var result = useCase.listAthleteHallOfFame(ATHLETE);

    assertThat(result.athleteId()).isEqualTo(ATHLETE);
    assertThat(result.items()).containsExactly(latest, older);
    assertThat(result.updatedAt()).isEqualTo(Instant.parse("2026-10-15T18:00:00Z"));
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void anAthleteWithoutResultsHasNoLastModification() {
    when(hallOfFameRepository.findByAthleteCode(ATHLETE)).thenReturn(List.of());

    final var result = useCase.listAthleteHallOfFame(ATHLETE);

    assertThat(result.items()).isEmpty();
    assertThat(result.updatedAt()).isNull();
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void anUnavailableStorageIsPropagated() {
    when(hallOfFameRepository.findByAthleteCode(ATHLETE))
        .thenThrow(new StorageUnavailableException("Database unavailable", null));

    assertThatThrownBy(() -> useCase.listAthleteHallOfFame(ATHLETE))
        .isInstanceOf(StorageUnavailableException.class);
  }

  private static HallOfFameEntry entry(final String id, final String date, final String updatedAt) {
    return new HallOfFameEntry(
        id,
        LocalDate.parse(date),
        "Classic Physique",
        "Competition " + id,
        "5.º Puesto",
        null,
        null,
        List.of(),
        Instant.parse(updatedAt));
  }
}
