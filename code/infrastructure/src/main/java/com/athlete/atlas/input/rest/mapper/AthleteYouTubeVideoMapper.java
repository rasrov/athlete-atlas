package com.athlete.atlas.input.rest.mapper;

import com.athlete.atlas.infrastructure.api.model.YouTubeVideoCollection;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AthleteYouTubeVideoMapper {

  com.athlete.atlas.infrastructure.api.model.YouTubeVideo toDto(YouTubeVideo youTubeVideo);

  default OffsetDateTime toOffsetDateTime(final Instant instant) {
    return SelectionMetadataMapper.toOffsetDateTime(instant);
  }

  /** Keeps the order of the selection (latest first) in the set required by uniqueItems. */
  default YouTubeVideoCollection toDtoCollection(final AthleteSelection<YouTubeVideo> selection) {
    final var data = new LinkedHashSet<com.athlete.atlas.infrastructure.api.model.YouTubeVideo>();
    selection.items().forEach(video -> data.add(toDto(video)));
    return new YouTubeVideoCollection(data, SelectionMetadataMapper.toMetadata(selection));
  }
}
