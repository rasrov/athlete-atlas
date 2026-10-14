package com.athlete.atlas.output.rest.instagram.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.athlete.atlas.model.instagram.InstagramMediaType;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.output.rest.instagram.generated.model.Media;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;

class InstagramPostMapperTest {

  private static final String IMAGE_URL = "https://scontent.cdninstagram.com/v/image.jpg?sig=abc";

  private static final String THUMBNAIL_URL = "https://video.xx.fbcdn.net/v/thumb.jpg?sig=abc";

  private final InstagramPostMapper mapper = Mappers.getMapper(InstagramPostMapper.class);

  @Test
  void mapsAnImagePost() {
    final var media =
        media("IMAGE")
            .permalink("https://www.instagram.com/p/AbC_12-x/")
            .timestamp("2026-10-01T12:00:00+0000");

    assertThat(mapper.toDomain(media))
        .isEqualTo(
            new InstagramPost(
                "12345678901234567",
                InstagramMediaType.IMAGE,
                Instant.parse("2026-10-01T12:00:00Z"),
                URI.create("https://www.instagram.com/p/AbC_12-x/"),
                URI.create(IMAGE_URL)));
  }

  @Test
  void usesTheMediaUrlAsCoverOfImagesAndCarousels() {
    assertThat(mapper.toDomain(media("CAROUSEL_ALBUM")).previewImageUrl())
        .isEqualTo(URI.create(IMAGE_URL));
    assertThat(mapper.toDomain(media("IMAGE")).previewImageUrl()).isEqualTo(URI.create(IMAGE_URL));
  }

  @Test
  void usesTheThumbnailUrlAsCoverOfVideos() {
    final var video = mapper.toDomain(media("VIDEO").permalink("https://www.instagram.com/reel/AbC/"));

    assertThat(video.mediaType()).isEqualTo(InstagramMediaType.VIDEO);
    assertThat(video.previewImageUrl()).isEqualTo(URI.create(THUMBNAIL_URL));
  }

  @Test
  void keepsAPostWithoutCover() {
    assertThat(mapper.toDomain(media("VIDEO").thumbnailUrl(null)).previewImageUrl()).isNull();
    assertThat(mapper.toDomain(media("IMAGE").mediaUrl(null)).previewImageUrl()).isNull();
  }

  @ParameterizedTest
  @CsvSource({
    "2026-10-01T12:00:00+0000, 2026-10-01T12:00:00Z",
    "2026-10-01T12:00:00+00:00, 2026-10-01T12:00:00Z",
    "2026-10-01T12:00:00Z, 2026-10-01T12:00:00Z",
    "2026-10-01T14:00:00+0200, 2026-10-01T12:00:00Z",
    "2026-10-01T14:00:00+02:00, 2026-10-01T12:00:00Z",
    "2026-10-01T12:00:00.500+0000, 2026-10-01T12:00:00.500Z"
  })
  void readsTimestampsWithAnyOffsetFormat(final String timestamp, final Instant expected) {
    assertThat(InstagramPostValues.publishedAt(timestamp)).contains(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2026-10-01T12:00:00",
        "2026-10-01",
        "2026-02-30T12:00:00+0000",
        "2026-10-01 12:00:00+0000",
        "invalid"
      })
  void rejectsTimestampsThatAreInvalidOrWithoutOffset(final String timestamp) {
    assertThat(InstagramPostValues.publishedAt(timestamp)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"https://www.instagram.com/p/AbC_12-x/", "https://www.instagram.com/reel/AbC/"})
  void acceptsPostAndReelPermalinks(final String permalink) {
    assertThat(InstagramPostValues.permalink(permalink)).contains(URI.create(permalink));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://www.instagram.com/p/AbC/",
        "https://instagram.com/p/AbC/",
        "https://www.instagram.com/p/AbC",
        "https://www.instagram.com/p/AbC/?igsh=1",
        "https://www.instagram.com/p/AbC/#comments",
        "https://www.instagram.com/tv/AbC/",
        "https://www.instagram.com/p/AbC/extra/",
        "https://user@www.instagram.com/p/AbC/",
        "https://www.instagram.com:443/p/AbC/",
        "https://www.instagram.com.evil.com/p/AbC/",
        "not a url"
      })
  void rejectsOtherPermalinks(final String permalink) {
    assertThat(InstagramPostValues.permalink(permalink)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://cdninstagram.com/a.jpg",
        "https://scontent-mad1-1.cdninstagram.com/a.jpg?sig=1",
        "https://fbcdn.net/a.jpg",
        "https://video.xx.FBCDN.net:443/a.jpg"
      })
  void acceptsCoversOnTheInstagramCdns(final String url) {
    assertThat(InstagramPostValues.previewImageUrl(InstagramMediaType.IMAGE, url, null))
        .contains(URI.create(url));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://scontent.cdninstagram.com/a.jpg",
        "https://evilfbcdn.net/a.jpg",
        "https://fbcdn.net.evil.com/a.jpg",
        "https://evil.com/fbcdn.net/a.jpg",
        "https://user:pass@scontent.cdninstagram.com/a.jpg",
        "https://scontent.cdninstagram.com:8443/a.jpg",
        "https://scontent.cdninstagram.com/a b.jpg"
      })
  void rejectsOtherCovers(final String url) {
    assertThat(InstagramPostValues.previewImageUrl(InstagramMediaType.IMAGE, url, null)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"IMAGE", "VIDEO", "CAROUSEL_ALBUM"})
  void supportsImagesVideosAndCarousels(final String type) {
    assertThat(InstagramPostValues.mediaType(type)).contains(InstagramMediaType.valueOf(type));
  }

  @ParameterizedTest
  @ValueSource(strings = {"STORY", "image", ""})
  void doesNotSupportOtherMediaTypes(final String type) {
    assertThat(InstagramPostValues.mediaType(type)).isEmpty();
  }

  private static Media media(final String mediaType) {
    return new Media()
        .id("12345678901234567")
        .mediaType(mediaType)
        .permalink("https://www.instagram.com/p/AbC/")
        .mediaUrl(IMAGE_URL)
        .thumbnailUrl(THUMBNAIL_URL)
        .timestamp("2026-10-01T12:00:00+0000");
  }
}
