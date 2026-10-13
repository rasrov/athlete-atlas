package com.athlete.atlas.input.rest.mapper;

import com.athlete.atlas.infrastructure.api.model.Competition;
import com.athlete.atlas.infrastructure.api.model.CompetitionCollection;
import com.athlete.atlas.infrastructure.api.model.Metadata;
import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.competition.enums.ParticipationStatus;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AthleteCompetitionMapper {

  ParticipationStatus toDomain(
      com.athlete.atlas.infrastructure.api.model.ParticipationStatus participationStatus);

  List<Competition> toDtoList(List<AthleteCompetition> athleteCompetitions);

  @Mapping(target = "participation.status", source = "status")
  Competition toDto(AthleteCompetition athleteCompetition);

  /**
   * The acquisition time of the competitions is not known yet, so updatedAt is null and
   * availability is unknown, as the contract defines for data of unknown age.
   */
  default CompetitionCollection toDtoCollection(
      final String athleteId, final List<AthleteCompetition> athleteCompetitions) {
    return new CompetitionCollection(
        toDtoList(athleteCompetitions),
        new Metadata(athleteId, null, Metadata.AvailabilityEnum.UNKNOWN));
  }
}
