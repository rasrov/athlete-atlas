package com.athlete.atlas.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.athlete.atlas.config.CompetitionCatalogProperties;
import com.athlete.atlas.exception.DataUnavailableException;
import com.athlete.atlas.exception.NotFoundException;
import com.athlete.atlas.exception.RemoteException;
import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.athlete.Athlete;
import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.CompetitionCatalog;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.port.output.AthleteRepository;
import com.athlete.atlas.port.output.CompetitionCatalogRepository;
import com.athlete.atlas.port.output.CompetitionParticipationRepository;
import com.athlete.atlas.port.output.CompetitionPort;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AthleteCompetitionUseCaseImplTest {

  private static final String ATHLETE_ID = "kim-angel";

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

  private static final Duration FRESHNESS_THRESHOLD = Duration.ofHours(12);

  private CompetitionPort competitionPort;

  private InMemoryRepository repository;

  private AthleteRepository athleteRepository;

  private CompetitionParticipationRepository participationRepository;

  @BeforeEach
  void setUp() {
    competitionPort = mock(CompetitionPort.class);
    repository = new InMemoryRepository();
    athleteRepository = mock(AthleteRepository.class);
    participationRepository = mock(CompetitionParticipationRepository.class);
    when(athleteRepository.findByCode(ATHLETE_ID)).thenReturn(Optional.of(new Athlete(ATHLETE_ID, null, null)));
    when(participationRepository.findConfirmedCompetitionIds(ATHLETE_ID))
        .thenReturn(Set.of("ifbb-1"));
  }

  @ParameterizedTest
  @CsvSource({
    "2026-09-30, 2025-10-01, 2026-10-01",
    "2026-10-01, 2026-10-01, 2027-10-01",
    "2027-03-15, 2026-10-01, 2027-10-01"
  })
  void refreshesTheSeasonOfTheCurrentDate(
      final LocalDate today, final LocalDate expectedStart, final LocalDate expectedEnd) {
    useCaseAt(at(today)).refresh();

    verify(competitionPort).fetchCompetitions(new DateRange(expectedStart, expectedEnd));
  }

  @Test
  void isUnavailableUntilTheFirstSuccessfulRefresh() {
    assertThatThrownBy(() -> useCaseAt(at(TODAY)).listAthleteCompetitions(ATHLETE_ID))
        .isInstanceOf(DataUnavailableException.class);
    verifyNoInteractions(competitionPort);
  }

  @Test
  void servesTheRefreshedCatalogWithoutCallingTheProvider() {
    final var competitions = List.of(competition());
    when(competitionPort.fetchCompetitions(DateRange.seasonOf(TODAY))).thenReturn(competitions);
    final var useCase = useCaseAt(at(TODAY));
    useCase.refresh();

    final var result = useCase.listAthleteCompetitions(ATHLETE_ID);
    useCase.listAthleteCompetitions(ATHLETE_ID);

    assertThat(result.athleteId()).isEqualTo(ATHLETE_ID);
    assertThat(result.items()).isEqualTo(competitions);
    assertThat(result.updatedAt()).isEqualTo(at(TODAY).instant());
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
    verify(competitionPort).fetchCompetitions(any());
  }

  @Test
  void anEmptySeasonIsValidData() {
    when(competitionPort.fetchCompetitions(any())).thenReturn(List.of());
    final var useCase = useCaseAt(at(TODAY));
    useCase.refresh();

    assertThat(useCase.listAthleteCompetitions(ATHLETE_ID).items()).isEmpty();
  }

  @Test
  void aFailedRefreshKeepsThePreviousCatalogAsStale() {
    final var competitions = List.of(competition());
    when(competitionPort.fetchCompetitions(any()))
        .thenReturn(competitions)
        .thenThrow(new RemoteException("[ifbb_events_api] upstream error", 502, null));
    final var useCase = useCaseAt(at(TODAY));
    useCase.refresh();

    useCase.refresh();

    final var result = useCase.listAthleteCompetitions(ATHLETE_ID);
    assertThat(result.items()).isEqualTo(competitions);
    assertThat(result.updatedAt()).isEqualTo(at(TODAY).instant());
    assertThat(result.availability()).isEqualTo(Availability.STALE);
  }

  @Test
  void anUnexpectedErrorDuringTheRefreshDoesNotPropagate() {
    when(competitionPort.fetchCompetitions(any())).thenThrow(new IllegalStateException("bug"));

    useCaseAt(at(TODAY)).refresh();

    assertThat(repository.find()).isEmpty();
  }

  @Test
  void aCatalogOlderThanTheThresholdIsStale() {
    when(competitionPort.fetchCompetitions(any())).thenReturn(List.of(competition()));
    useCaseAt(at(TODAY)).refresh();

    final var later = Clock.offset(at(TODAY), FRESHNESS_THRESHOLD.plusMinutes(1));

    assertThat(useCaseAt(later).listAthleteCompetitions(ATHLETE_ID).availability())
        .isEqualTo(Availability.STALE);
  }

  @Test
  void theCatalogOfThePreviousSeasonIsNotServed() {
    when(competitionPort.fetchCompetitions(any())).thenReturn(List.of(competition()));
    useCaseAt(at(LocalDate.of(2026, 9, 30))).refresh();

    final var nextSeason = useCaseAt(at(LocalDate.of(2026, 10, 1)));

    assertThatThrownBy(() -> nextSeason.listAthleteCompetitions(ATHLETE_ID))
        .isInstanceOf(DataUnavailableException.class);
  }

  @Test
  void returnsOnlyTheCompetitionsConfirmedByTheAthlete() {
    when(competitionPort.fetchCompetitions(any()))
        .thenReturn(List.of(competition("ifbb-1"), competition("ifbb-2"), competition("ifbb-3")));
    when(participationRepository.findConfirmedCompetitionIds(ATHLETE_ID))
        .thenReturn(Set.of("ifbb-3", "ifbb-1", "ifbb-other-season"));
    final var useCase = useCaseAt(at(TODAY));
    useCase.refresh();

    final var result = useCase.listAthleteCompetitions(ATHLETE_ID);

    assertThat(result.items())
        .extracting(AthleteCompetition::id)
        .containsExactly("ifbb-1", "ifbb-3");
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void anAthleteWithoutConfirmationsHasAnEmptyCalendar() {
    when(competitionPort.fetchCompetitions(any())).thenReturn(List.of(competition()));
    when(participationRepository.findConfirmedCompetitionIds(ATHLETE_ID)).thenReturn(Set.of());
    final var useCase = useCaseAt(at(TODAY));
    useCase.refresh();

    final var result = useCase.listAthleteCompetitions(ATHLETE_ID);

    assertThat(result.items()).isEmpty();
    assertThat(result.availability()).isEqualTo(Availability.AVAILABLE);
  }

  @Test
  void anUnknownAthleteIsNotFound() {
    when(athleteRepository.findByCode("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCaseAt(at(TODAY)).listAthleteCompetitions("unknown"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void servesAnEmptyCalendarOfUnknownAvailabilityWhenTheStorageIsUnavailable() {
    when(competitionPort.fetchCompetitions(any())).thenReturn(List.of(competition()));
    when(participationRepository.findConfirmedCompetitionIds(ATHLETE_ID))
        .thenThrow(new StorageUnavailableException("down", null));
    final var useCase = useCaseAt(at(TODAY));
    useCase.refresh();

    final var result = useCase.listAthleteCompetitions(ATHLETE_ID);

    assertThat(result.items()).isEmpty();
    assertThat(result.updatedAt()).isNull();
    assertThat(result.availability()).isEqualTo(Availability.UNKNOWN);
  }

  @Test
  void anUnavailableStorageWhileCheckingTheAthleteAlsoServesAnEmptyCalendar() {
    when(athleteRepository.findByCode(ATHLETE_ID))
        .thenThrow(new StorageUnavailableException("down", null));

    assertThat(useCaseAt(at(TODAY)).listAthleteCompetitions(ATHLETE_ID).availability())
        .isEqualTo(Availability.UNKNOWN);
  }

  private AthleteCompetitionUseCaseImpl useCaseAt(final Clock clock) {
    return new AthleteCompetitionUseCaseImpl(
        competitionPort,
        repository,
        new CompetitionCatalogProperties(FRESHNESS_THRESHOLD),
        athleteRepository,
        participationRepository,
        clock);
  }

  private static Clock at(final LocalDate today) {
    return Clock.fixed(today.atTime(6, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  }

  private static AthleteCompetition competition() {
    return competition("ifbb-1");
  }

  private static AthleteCompetition competition(final String id) {
    return new AthleteCompetition(
        id,
        "Example Pro",
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 11, 1),
        "Madrid",
        "Spain",
        "Classic Physique",
        URI.create("https://www.ifbbpro.com/competition/example/"),
        "1",
        null);
  }

  private static final class InMemoryRepository implements CompetitionCatalogRepository {

    private final AtomicReference<CompetitionCatalog> catalog = new AtomicReference<>();

    @Override
    public Optional<CompetitionCatalog> find() {
      return Optional.ofNullable(catalog.get());
    }

    @Override
    public void save(final CompetitionCatalog competitionCatalog) {
      catalog.set(competitionCatalog);
    }
  }
}
