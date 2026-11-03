package com.athlete.atlas.output.persistence.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Pins the stored JSON format. The JSON literals are the format written by the current version:
 * a new version (for example, a Jackson upgrade) must keep reading them without changing them, and
 * must write the same fields with the same values (the field order does not matter).
 */
class ContentJsonTest {

  private static final String STORED_YOUTUBE_VIDEOS =
      """
      [{"id":"a","title":"Video a","publishedAt":"2026-10-13T06:00:00Z",\
      "watchUrl":"https://www.youtube.com/watch?v=a",\
      "embedUrl":"https://www.youtube-nocookie.com/embed/a",\
      "thumbnailUrl":"https://i.ytimg.com/vi/a/hqdefault.jpg"},\
      {"id":"b","title":"Video b","publishedAt":"2026-10-12T18:30:15.123Z",\
      "watchUrl":"https://www.youtube.com/watch?v=b",\
      "embedUrl":"https://www.youtube-nocookie.com/embed/b",\
      "thumbnailUrl":null}]""";

  private static final String STORED_INSTAGRAM_POSTS =
      """
      [{"id":"17890000000000001","mediaType":"CAROUSEL_ALBUM",\
      "publishedAt":"2026-09-29T10:00:00Z","permalink":"https://www.instagram.com/p/C1/",\
      "previewImageUrl":null}]""";

  private static final String STORED_COMPETITIONS =
      """
      [{"id":"ifbb-1","name":"Competition ifbb-1","startDate":"2027-03-01",\
      "endDate":"2027-03-02","city":"Madrid","country":"Spain",\
      "description":"Classic Physique","sourceUrl":"https://ifbbpro.com/competition/ifbb-1",\
      "sourceId":"ifbb-1","logoUrl":null},\
      {"id":"ifbb-2","name":"Competition ifbb-2","startDate":"2027-03-01",\
      "endDate":"2027-03-02","city":null,"country":null,"description":null,\
      "sourceUrl":"https://ifbbpro.com/competition/ifbb-2","sourceId":"ifbb-2","logoUrl":null}]""";

  private static final List<StoredYouTubeVideo> YOUTUBE_VIDEOS =
      List.of(
          new StoredYouTubeVideo(
              "a",
              "Video a",
              Instant.parse("2026-10-13T06:00:00Z"),
              URI.create("https://www.youtube.com/watch?v=a"),
              URI.create("https://www.youtube-nocookie.com/embed/a"),
              URI.create("https://i.ytimg.com/vi/a/hqdefault.jpg")),
          new StoredYouTubeVideo(
              "b",
              "Video b",
              Instant.parse("2026-10-12T18:30:15.123Z"),
              URI.create("https://www.youtube.com/watch?v=b"),
              URI.create("https://www.youtube-nocookie.com/embed/b"),
              null));

  private static final List<StoredInstagramPost> INSTAGRAM_POSTS =
      List.of(
          new StoredInstagramPost(
              "17890000000000001",
              "CAROUSEL_ALBUM",
              Instant.parse("2026-09-29T10:00:00Z"),
              URI.create("https://www.instagram.com/p/C1/"),
              null));

  private static final List<StoredCompetition> COMPETITIONS =
      List.of(
          new StoredCompetition(
              "ifbb-1",
              "Competition ifbb-1",
              LocalDate.of(2027, 3, 1),
              LocalDate.of(2027, 3, 2),
              "Madrid",
              "Spain",
              "Classic Physique",
              URI.create("https://ifbbpro.com/competition/ifbb-1"),
              "ifbb-1",
              null),
          new StoredCompetition(
              "ifbb-2",
              "Competition ifbb-2",
              LocalDate.of(2027, 3, 1),
              LocalDate.of(2027, 3, 2),
              null,
              null,
              null,
              URI.create("https://ifbbpro.com/competition/ifbb-2"),
              "ifbb-2",
              null));

  @Test
  void readsTheStoredYouTubeVideos() throws Exception {
    assertThat(ContentJson.readList(STORED_YOUTUBE_VIDEOS, StoredYouTubeVideo.class))
        .isEqualTo(YOUTUBE_VIDEOS);
  }

  @Test
  void readsTheStoredInstagramPosts() throws Exception {
    assertThat(ContentJson.readList(STORED_INSTAGRAM_POSTS, StoredInstagramPost.class))
        .isEqualTo(INSTAGRAM_POSTS);
  }

  @Test
  void readsTheStoredCompetitions() throws Exception {
    assertThat(ContentJson.readList(STORED_COMPETITIONS, StoredCompetition.class))
        .isEqualTo(COMPETITIONS);
  }

  @Test
  void ignoresStoredFieldsThatNoLongerExist() throws Exception {
    final var json =
        """
        [{"id":"17890000000000001","mediaType":"IMAGE","publishedAt":"2026-09-29T10:00:00Z",\
        "permalink":"https://www.instagram.com/p/C1/","previewImageUrl":null,"caption":"old"}]""";

    assertThat(ContentJson.readList(json, StoredInstagramPost.class))
        .containsExactly(
            new StoredInstagramPost(
                "17890000000000001",
                "IMAGE",
                Instant.parse("2026-09-29T10:00:00Z"),
                URI.create("https://www.instagram.com/p/C1/"),
                null));
  }

  @Test
  void readsAnEmptyList() throws Exception {
    assertThat(ContentJson.readList("[]", StoredYouTubeVideo.class)).isEmpty();
  }

  @Test
  void writesTheStoredYouTubeFormat() throws Exception {
    JSONAssert.assertEquals(
        STORED_YOUTUBE_VIDEOS, ContentJson.write(YOUTUBE_VIDEOS), JSONCompareMode.STRICT);
  }

  @Test
  void writesTheStoredInstagramFormat() throws Exception {
    JSONAssert.assertEquals(
        STORED_INSTAGRAM_POSTS, ContentJson.write(INSTAGRAM_POSTS), JSONCompareMode.STRICT);
  }

  @Test
  void writesTheStoredCompetitionFormat() throws Exception {
    JSONAssert.assertEquals(
        STORED_COMPETITIONS, ContentJson.write(COMPETITIONS), JSONCompareMode.STRICT);
  }
}
