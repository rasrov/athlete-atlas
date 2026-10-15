package com.athlete.atlas.input.rest.mapper;

import com.athlete.atlas.infrastructure.api.model.Competition;
import com.athlete.atlas.infrastructure.api.model.CompetitionCollection;
import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.selection.AthleteSelection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AthleteCompetitionMapper {

  List<Competition> toDtoList(List<AthleteCompetition> athleteCompetitions);

  Competition toDto(AthleteCompetition athleteCompetition);

  default CompetitionCollection toDtoCollection(
      final AthleteSelection<AthleteCompetition> selection) {
    return new CompetitionCollection(
        toDtoList(selection.items()), SelectionMetadataMapper.toMetadata(selection));
  }
}
