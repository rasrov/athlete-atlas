package com.athlete.atlas.output.rest.ifbb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.athlete.atlas.output.rest.ifbb.client.IfbbEventsClient;
import com.athlete.atlas.output.rest.ifbb.config.IfbbProClientProperties;
import com.athlete.atlas.output.rest.ifbb.mapper.IfbbEventMapper;
import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.service.InvokerServiceImpl;
import com.athlete.atlas.service.RemotePage;
import com.athlete.atlas.output.rest.ifbb.generated.model.Event;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class IfbbProCompetitionAdapterTest {

  private static final DateRange SEASON = DateRange.seasonOf(LocalDate.of(2026, 10, 1));

  private static final int PAGE_SIZE = 50;

  private IfbbEventsClient ifbbEventsClient;

  private IfbbProCompetitionAdapter adapter;

  @BeforeEach
  void setUp() {
    ifbbEventsClient = mock(IfbbEventsClient.class);
    adapter =
        new IfbbProCompetitionAdapter(
            ifbbEventsClient,
            new InvokerServiceImpl(),
            Mappers.getMapper(IfbbEventMapper.class),
            new IfbbProClientProperties(null, null, null, null, PAGE_SIZE, 200));
  }

  @Test
  void keepsTheFirstValidOccurrenceOfDuplicateIds() {
    givenEvents(
        event(1L, "2026-11-01 00:00:00", "2026-11-01 23:59:59").title("First"),
        event(1L, "2026-11-02 00:00:00", "2026-11-02 23:59:59").title("Duplicate"));

    final var competitions = adapter.fetchCompetitions(SEASON);

    assertThat(competitions).extracting(AthleteCompetition::name).containsExactly("First");
  }

  @Test
  void discardsInvalidEventsWithoutFailing() {
    givenEvents(
        event(null, "2026-11-01 00:00:00", "2026-11-01 23:59:59"),
        event(-3L, "2026-11-01 00:00:00", "2026-11-01 23:59:59"),
        event(4L, "2026-02-30 00:00:00", "2026-11-01 23:59:59"),
        event(5L, null, "2026-11-01 23:59:59"),
        event(6L, "2026-11-05 00:00:00", "2026-11-01 23:59:59"),
        event(7L, "2026-11-01 00:00:00", "2026-11-01 23:59:59").url(null),
        event(8L, "2026-11-01 00:00:00", "2026-11-01 23:59:59"));

    final var competitions = adapter.fetchCompetitions(SEASON);

    assertThat(competitions).extracting(AthleteCompetition::id).containsExactly("ifbb-8");
  }

  @Test
  void anInvalidFirstOccurrenceDoesNotHideAValidDuplicate() {
    givenEvents(
        event(9L, "invalid", "2026-11-01 23:59:59"),
        event(9L, "2026-11-01 00:00:00", "2026-11-01 23:59:59"));

    assertThat(adapter.fetchCompetitions(SEASON))
        .extracting(AthleteCompetition::id)
        .containsExactly("ifbb-9");
  }

  @Test
  void keepsOnlyEventsOverlappingTheRange() {
    givenEvents(
        event(1L, "2026-09-29 00:00:00", "2026-09-30 23:59:59"),
        event(2L, "2026-09-30 00:00:00", "2026-10-01 23:59:59"),
        event(3L, "2027-09-30 00:00:00", "2027-09-30 23:59:59"),
        event(4L, "2027-10-01 00:00:00", "2027-10-02 23:59:59"));

    assertThat(adapter.fetchCompetitions(SEASON))
        .extracting(AthleteCompetition::id)
        .containsExactly("ifbb-2", "ifbb-3");
  }

  @Test
  void sortsByStartDateThenNumericSourceId() {
    givenEvents(
        event(10L, "2026-11-01 00:00:00", "2026-11-01 23:59:59"),
        event(9L, "2026-11-01 00:00:00", "2026-11-01 23:59:59"),
        event(1L, "2026-12-01 00:00:00", "2026-12-01 23:59:59"));

    assertThat(adapter.fetchCompetitions(SEASON))
        .extracting(AthleteCompetition::id)
        .containsExactly("ifbb-9", "ifbb-10", "ifbb-1");
  }

  private void givenEvents(final Event... events) {
    when(ifbbEventsClient.fetchPage(any(DateRange.class), eq(PAGE_SIZE), anyInt()))
        .thenReturn(new RemotePage<>(Arrays.asList(events), events.length, 1));
  }

  private static Event event(final Long id, final String startDate, final String endDate) {
    return new Event()
        .id(id)
        .title("Event " + id)
        .description("<p>Classic Physique</p>")
        .url(URI.create("https://www.ifbbpro.com/competition/" + id))
        .categories(List.of())
        .startDate(startDate)
        .endDate(endDate);
  }
}
