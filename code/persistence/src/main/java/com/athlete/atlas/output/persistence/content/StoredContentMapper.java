package com.athlete.atlas.output.persistence.content;

import com.athlete.atlas.model.competition.AthleteCompetition;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StoredContentMapper {

  StoredCompetition toStored(AthleteCompetition competition);

  AthleteCompetition toDomain(StoredCompetition competition);

  StoredYouTubeVideo toStored(YouTubeVideo video);

  YouTubeVideo toDomain(StoredYouTubeVideo video);

  StoredInstagramPost toStored(InstagramPost post);

  InstagramPost toDomain(StoredInstagramPost post);
}
