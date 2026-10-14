package com.athlete.atlas.input.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.athlete.atlas.infrastructure.api.model.Metadata;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.youtube.AthleteYouTubeVideos;
import com.athlete.atlas.model.youtube.YouTubeVideo;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;

class AthleteYouTubeVideoMapperTest {

  private static final Instant UPDATED_AT = Instant.parse("2026-10-14T06:00:00Z");

  private final AthleteYouTubeVideoMapper mapper =
      Mappers.getMapper(AthleteYouTubeVideoMapper.class);

  @Test
  void mapsEveryFieldOfAVideo() {
    final var dto = mapper.toDto(video("AbCdEfGhI00", "2026-10-07T12:00:00Z", "https://i.ytimg.com/a.jpg"));

    assertThat(dto)
        .isEqualTo(
            new com.athlete.atlas.infrastructure.api.model.YouTubeVideo(
                "AbCdEfGhI00",
                "Video AbCdEfGhI00",
                OffsetDateTime.parse("2026-10-07T12:00:00Z"),
                URI.create("https://www.youtube.com/watch?v=AbCdEfGhI00"),
                URI.create("https://www.youtube-nocookie.com/embed/AbCdEfGhI00"),
                URI.create("https://i.ytimg.com/a.jpg")));
  }

  @Test
  void keepsAMissingThumbnailAsNull() {
    assertThat(mapper.toDto(video("AbCdEfGhI00", "2026-10-07T12:00:00Z", null)).getThumbnailUrl())
        .isNull();
  }

  @ParameterizedTest
  @EnumSource(Availability.class)
  void mapsEveryAvailability(final Availability availability) {
    assertThat(mapper.toDto(availability).name()).isEqualTo(availability.name());
  }

  @Test
  void buildsTheCollectionKeepingTheOrderAndTheMetadata() {
    final var latest = video("AbCdEfGhI01", "2026-10-08T12:00:00Z", null);
    final var older = video("AbCdEfGhI00", "2026-10-07T12:00:00Z", null);

    final var collection =
        mapper.toDtoCollection(
            new AthleteYouTubeVideos(
                "kim-angel", List.of(latest, older), UPDATED_AT, Availability.STALE));

    assertThat(collection.getData())
        .extracting(com.athlete.atlas.infrastructure.api.model.YouTubeVideo::getId)
        .containsExactly("AbCdEfGhI01", "AbCdEfGhI00");
    assertThat(collection.getMeta())
        .isEqualTo(
            new Metadata(
                "kim-angel",
                OffsetDateTime.parse("2026-10-14T06:00:00Z"),
                Metadata.AvailabilityEnum.STALE));
  }

  @Test
  void anEmptySelectionIsAnEmptyCollection() {
    final var collection =
        mapper.toDtoCollection(
            new AthleteYouTubeVideos("kim-angel", List.of(), UPDATED_AT, Availability.AVAILABLE));

    assertThat(collection.getData()).isEmpty();
  }

  private static YouTubeVideo video(final String id, final String publishedAt, final String thumbnail) {
    return new YouTubeVideo(
        id,
        "Video " + id,
        Instant.parse(publishedAt),
        URI.create("https://www.youtube.com/watch?v=" + id),
        URI.create("https://www.youtube-nocookie.com/embed/" + id),
        thumbnail == null ? null : URI.create(thumbnail));
  }
}
