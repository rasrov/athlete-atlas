package com.athlete.atlas.input.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.athlete.atlas.infrastructure.api.model.Metadata;
import com.athlete.atlas.model.enums.Availability;
import com.athlete.atlas.model.instagram.InstagramMediaType;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.model.selection.AthleteSelection;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;

class AthleteInstagramPostMapperTest {

  private static final Instant UPDATED_AT = Instant.parse("2026-10-14T06:00:00Z");

  private final AthleteInstagramPostMapper mapper =
      Mappers.getMapper(AthleteInstagramPostMapper.class);

  @Test
  void mapsEveryFieldOfAPost() {
    final var dto =
        mapper.toDto(post("123", InstagramMediaType.CAROUSEL_ALBUM, "https://x.fbcdn.net/a.jpg"));

    assertThat(dto)
        .isEqualTo(
            new com.athlete.atlas.infrastructure.api.model.InstagramPost(
                "123",
                com.athlete.atlas.infrastructure.api.model.InstagramPost.MediaTypeEnum
                    .CAROUSEL_ALBUM,
                OffsetDateTime.parse("2026-10-13T10:00:00Z"),
                URI.create("https://www.instagram.com/p/C123/"),
                URI.create("https://x.fbcdn.net/a.jpg")));
  }

  @ParameterizedTest
  @EnumSource(InstagramMediaType.class)
  void mapsEveryMediaType(final InstagramMediaType mediaType) {
    assertThat(mapper.toDto(post("1", mediaType, null)).getMediaType().name())
        .isEqualTo(mediaType.name());
  }

  @Test
  void keepsAMissingPreviewAsNull() {
    assertThat(mapper.toDto(post("1", InstagramMediaType.VIDEO, null)).getPreviewImageUrl())
        .isNull();
  }

  @Test
  void buildsTheCollectionKeepingTheOrderAndTheMetadata() {
    final var collection =
        mapper.toDtoCollection(
            new AthleteSelection<>(
                "kim-angel",
                List.of(
                    post("2", InstagramMediaType.IMAGE, null),
                    post("1", InstagramMediaType.VIDEO, null)),
                UPDATED_AT,
                Availability.AVAILABLE));

    assertThat(collection.getData())
        .extracting(com.athlete.atlas.infrastructure.api.model.InstagramPost::getId)
        .containsExactly("2", "1");
    assertThat(collection.getMeta())
        .isEqualTo(
            new Metadata(
                "kim-angel",
                OffsetDateTime.parse("2026-10-14T06:00:00Z"),
                Metadata.AvailabilityEnum.AVAILABLE));
  }

  private static InstagramPost post(
      final String id, final InstagramMediaType mediaType, final String preview) {
    return new InstagramPost(
        id,
        mediaType,
        Instant.parse("2026-10-13T10:00:00Z"),
        URI.create("https://www.instagram.com/p/C" + id + "/"),
        preview == null ? null : URI.create(preview));
  }
}
