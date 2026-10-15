package com.athlete.atlas.input.rest.mapper;

import com.athlete.atlas.infrastructure.api.model.Metadata;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.selection.AthleteSelection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Metadata of the synchronized data (competitions, YouTube, Instagram) in the API contract. */
public final class SelectionMetadataMapper {

  private SelectionMetadataMapper() {}

  public static Metadata toMetadata(final AthleteSelection<?> selection) {
    return new Metadata(
        selection.athleteId(),
        toOffsetDateTime(selection.updatedAt()),
        toDto(selection.availability()));
  }

  public static Metadata.AvailabilityEnum toDto(final Availability availability) {
    return Metadata.AvailabilityEnum.valueOf(availability.name());
  }

  /** API date-times are served in UTC. */
  public static OffsetDateTime toOffsetDateTime(final Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }
}
