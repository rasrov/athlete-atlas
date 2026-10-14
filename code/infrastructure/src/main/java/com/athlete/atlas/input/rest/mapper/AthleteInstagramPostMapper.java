package com.athlete.atlas.input.rest.mapper;

import com.athlete.atlas.infrastructure.api.model.InstagramPostCollection;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.model.selection.AthleteSelection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AthleteInstagramPostMapper {

  com.athlete.atlas.infrastructure.api.model.InstagramPost toDto(InstagramPost instagramPost);

  default OffsetDateTime toOffsetDateTime(final Instant instant) {
    return SelectionMetadataMapper.toOffsetDateTime(instant);
  }

  /** Keeps the order of the selection (latest first) in the set required by uniqueItems. */
  default InstagramPostCollection toDtoCollection(final AthleteSelection<InstagramPost> selection) {
    final var data = new LinkedHashSet<com.athlete.atlas.infrastructure.api.model.InstagramPost>();
    selection.items().forEach(post -> data.add(toDto(post)));
    return new InstagramPostCollection(data, SelectionMetadataMapper.toMetadata(selection));
  }
}
