package com.athlete.atlas.output.rest.youtube.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.output.rest.youtube.generated.model.Thumbnail;
import com.athlete.atlas.output.rest.youtube.generated.model.ThumbnailDetails;
import com.athlete.atlas.output.rest.youtube.generated.model.Video;
import com.athlete.atlas.output.rest.youtube.generated.model.VideoSnippet;
import com.athlete.atlas.output.rest.youtube.generated.model.VideoStatus;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class YouTubeVideoMapperTest {

  private final YouTubeVideoMapper mapper = Mappers.getMapper(YouTubeVideoMapper.class);

  @Test
  void mapsAProviderVideoToTheDomain() {
    final var video =
        video(new ThumbnailDetails().high(thumbnail("https://i.ytimg.com/vi/AbCdEfGhI00/hq.jpg")));

    assertThat(mapper.toDomain(video))
        .isEqualTo(
            new YouTubeVideo(
                "AbCdEfGhI00",
                "Example video",
                Instant.parse("2026-10-07T10:00:00Z"),
                URI.create("https://www.youtube.com/watch?v=AbCdEfGhI00"),
                URI.create("https://www.youtube-nocookie.com/embed/AbCdEfGhI00"),
                URI.create("https://i.ytimg.com/vi/AbCdEfGhI00/hq.jpg")));
  }

  @Test
  void usesTheHighestQualityThumbnailAvailable() {
    final var thumbnails =
        new ThumbnailDetails()
            ._default(thumbnail("https://i.ytimg.com/default.jpg"))
            .medium(thumbnail("https://i.ytimg.com/medium.jpg"))
            .high(thumbnail("https://i.ytimg.com/high.jpg"))
            .standard(thumbnail("https://i.ytimg.com/standard.jpg"));

    assertThat(mapper.toDomain(video(thumbnails)).thumbnailUrl())
        .isEqualTo(URI.create("https://i.ytimg.com/standard.jpg"));
  }

  @Test
  void skipsThumbnailsWithoutAValidHttpsUrl() {
    final var thumbnails =
        new ThumbnailDetails()
            .maxres(new Thumbnail())
            .standard(thumbnail("http://i.ytimg.com/standard.jpg"))
            .high(thumbnail("https://i.ytimg.com/not valid.jpg"))
            .medium(thumbnail("https://i.ytimg.com/medium.jpg"));

    assertThat(mapper.toDomain(video(thumbnails)).thumbnailUrl())
        .isEqualTo(URI.create("https://i.ytimg.com/medium.jpg"));
  }

  @Test
  void aVideoWithoutThumbnailsHasANullThumbnailUrl() {
    assertThat(mapper.toDomain(video(null)).thumbnailUrl()).isNull();
    assertThat(mapper.toDomain(video(new ThumbnailDetails())).thumbnailUrl()).isNull();
  }

  private static Video video(final ThumbnailDetails thumbnails) {
    return new Video()
        .id("AbCdEfGhI00")
        .snippet(
            new VideoSnippet()
                .channelId("UCEqAjtwYKSwKDN4BENhvc6A")
                .title("Example video")
                .publishedAt(OffsetDateTime.parse("2026-10-07T12:00:00+02:00"))
                .liveBroadcastContent("none")
                .thumbnails(thumbnails))
        .status(new VideoStatus().privacyStatus("public").embeddable(true));
  }

  private static Thumbnail thumbnail(final String url) {
    return new Thumbnail().url(url);
  }
}
