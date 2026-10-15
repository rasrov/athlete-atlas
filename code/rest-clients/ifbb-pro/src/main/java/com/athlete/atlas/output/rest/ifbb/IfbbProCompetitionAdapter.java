package com.athlete.atlas.output.rest.ifbb;

import static com.athlete.atlas.exception.RestClientErrorMapper.mapRemoteException;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.DateRange;
import com.athlete.atlas.output.rest.ifbb.client.IfbbEventsClient;
import com.athlete.atlas.output.rest.ifbb.config.IfbbProClientProperties;
import com.athlete.atlas.output.rest.ifbb.generated.model.Event;
import com.athlete.atlas.output.rest.ifbb.mapper.IfbbEventMapper;
import com.athlete.atlas.port.output.CompetitionPort;
import com.athlete.atlas.service.InvokerService;
import jakarta.annotation.Nonnull;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IfbbProCompetitionAdapter implements CompetitionPort {

  private static final Comparator<AthleteCompetition> BY_START_DATE_THEN_SOURCE_ID =
      Comparator.comparing(AthleteCompetition::startDate)
          .thenComparingLong(competition -> Long.parseLong(competition.sourceId()));

  private final IfbbEventsClient ifbbEventsClient;

  private final InvokerService invokerService;

  private final IfbbEventMapper ifbbEventMapper;

  private final IfbbProClientProperties ifbbProClientProperties;

  /**
   * Provider fields are optional, but the ones mapped to non-null domain fields must be present
   * before building the AthleteCompetition.
   */
  private static boolean hasRequiredFields(final Event event) {
    return event.getTitle() != null
        && event.getDescription() != null
        && event.getUrl() != null
        && event.getStartDate() != null
        && event.getEndDate() != null;
  }

  private static boolean isValid(final AthleteCompetition competition) {
    return !competition.endDate().isBefore(competition.startDate());
  }

  @Override
  public List<AthleteCompetition> fetchCompetitions(@Nonnull final DateRange range) {
    final var pageSize = ifbbProClientProperties.pageSize();
    final var events =
        invokerService.invokePaginated(
            page -> ifbbEventsClient.fetchPage(range, pageSize, page),
            pageSize,
            ifbbProClientProperties.maxPages(),
            e -> mapRemoteException(e, IfbbEventsClient.BOUNDED_CONTEXT));

    return toValidCompetitions(events).stream()
        .filter(competition -> range.overlaps(competition.startDate(), competition.endDate()))
        .sorted(BY_START_DATE_THEN_SOURCE_ID)
        .toList();
  }

  /**
   * Maps the provider events, discarding instead of failing on invalid ids, unmappable data and
   * duplicate ids (the first valid occurrence is kept).
   */
  private List<AthleteCompetition> toValidCompetitions(final List<Event> events) {
    final Set<Long> seenIds = new HashSet<>();
    final List<AthleteCompetition> competitions = new ArrayList<>(events.size());
    final List<Long> discardedIds = new ArrayList<>();

    for (final Event event : events) {
      toCompetition(event)
          .filter(competition -> seenIds.add(event.getId()))
          .ifPresentOrElse(competitions::add, () -> discardedIds.add(event.getId()));
    }

    if (!discardedIds.isEmpty()) {
      log.warn(
          "[{}] Discarded {} of {} events (invalid or duplicate). Ids: {}",
          IfbbEventsClient.BOUNDED_CONTEXT,
          discardedIds.size(),
          events.size(),
          discardedIds);
    }
    return competitions;
  }

  private Optional<AthleteCompetition> toCompetition(final Event event) {
    if (event.getId() == null || event.getId() <= 0 || !hasRequiredFields(event)) {
      return Optional.empty();
    }
    try {
      return Optional.of(ifbbEventMapper.toDomain(event))
          .filter(IfbbProCompetitionAdapter::isValid);
    } catch (final DateTimeException e) {
      log.debug(
          "[{}] Unparseable dates in event {}", IfbbEventsClient.BOUNDED_CONTEXT, event.getId(), e);
      return Optional.empty();
    }
  }
}
