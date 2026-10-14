package com.athlete.atlas.output.rest.youtube.mapper;

import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.output.rest.youtube.generated.model.Thumbnail;
import com.athlete.atlas.output.rest.youtube.generated.model.ThumbnailDetails;
import com.athlete.atlas.output.rest.youtube.generated.model.Video;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/** Maps provider videos to the domain. It only transforms data: eligibility is decided before. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface YouTubeVideoMapper {

  String WATCH_URL = "https://www.youtube.com/watch?v=";

  String EMBED_URL = "https://www.youtube-nocookie.com/embed/";

  @Mapping(target = "id", source = "id")
  @Mapping(target = "title", source = "snippet.title")
  @Mapping(target = "publishedAt", source = "snippet.publishedAt")
  @Mapping(target = "watchUrl", source = "id", qualifiedByName = "watchUrl")
  @Mapping(target = "embedUrl", source = "id", qualifiedByName = "embedUrl")
  @Mapping(target = "thumbnailUrl", source = "snippet.thumbnails", qualifiedByName = "thumbnailUrl")
  YouTubeVideo toDomain(Video video);

  default Instant toInstant(OffsetDateTime value) {
    return value == null ? null : value.toInstant();
  }

  @Named("watchUrl")
  default URI watchUrl(String videoId) {
    return URI.create(WATCH_URL + videoId);
  }

  @Named("embedUrl")
  default URI embedUrl(String videoId) {
    return URI.create(EMBED_URL + videoId);
  }

  /**
   * Highest quality thumbnail with a valid https URL, or null. maxres and standard are often
   * missing, so the smaller sizes are fallbacks.
   */
  @Named("thumbnailUrl")
  default URI thumbnailUrl(ThumbnailDetails thumbnails) {
    if (thumbnails == null) {
      return null;
    }
    return Stream.of(
            thumbnails.getMaxres(),
            thumbnails.getStandard(),
            thumbnails.getHigh(),
            thumbnails.getMedium(),
            thumbnails.getDefault())
        .filter(Objects::nonNull)
        .map(Thumbnail::getUrl)
        .map(YouTubeVideoMapper::httpsUri)
        .flatMap(Optional::stream)
        .findFirst()
        .orElse(null);
  }

  private static Optional<URI> httpsUri(final String value) {
    if (value == null || !value.startsWith("https://")) {
      return Optional.empty();
    }
    try {
      return Optional.of(new URI(value));
    } catch (final URISyntaxException e) {
      return Optional.empty();
    }
  }
}
