package com.athlete.atlas.input.rest.mapper;

import com.athlete.atlas.infrastructure.api.model.HallOfFameCollection;
import com.athlete.atlas.model.halloffame.HallOfFameEntry;
import com.athlete.atlas.model.halloffame.HallOfFameImage;
import com.athlete.atlas.model.selection.AthleteSelection;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AthleteHallOfFameMapper {

  com.athlete.atlas.infrastructure.api.model.HallOfFameEntry toDto(HallOfFameEntry entry);

  com.athlete.atlas.infrastructure.api.model.HallOfFameImage toDto(HallOfFameImage image);

  default HallOfFameCollection toDtoCollection(final AthleteSelection<HallOfFameEntry> selection) {
    return new HallOfFameCollection(
        selection.items().stream().map(this::toDto).toList(),
        SelectionMetadataMapper.toMetadata(selection));
  }
}
