package com.athlete.atlas.input.rest.mapper;

import com.athlete.atlas.infrastructure.api.model.Metadata;
import com.athlete.atlas.infrastructure.api.model.YouTubeVideoCollection;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.youtube.AthleteYouTubeVideos;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AthleteYouTubeVideoMapper {

  com.athlete.atlas.infrastructure.api.model.YouTubeVideo toDto(YouTubeVideo youTubeVideo);

  Metadata.AvailabilityEnum toDto(Availability availability);

  default OffsetDateTime toOffsetDateTime(final Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }

  /** Keeps the order of the selection (latest first) in the set required by uniqueItems. */
  default YouTubeVideoCollection toDtoCollection(final AthleteYouTubeVideos athleteYouTubeVideos) {
    final var data = new LinkedHashSet<com.athlete.atlas.infrastructure.api.model.YouTubeVideo>();
    athleteYouTubeVideos.videos().forEach(video -> data.add(toDto(video)));
    return new YouTubeVideoCollection(
        data,
        new Metadata(
            athleteYouTubeVideos.athleteId(),
            toOffsetDateTime(athleteYouTubeVideos.updatedAt()),
            toDto(athleteYouTubeVideos.availability())));
  }
}
