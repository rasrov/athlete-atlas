package com.athlete.atlas.output.persistence.content;

import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.port.output.YouTubeVideoSelectionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class YouTubeVideoSelectionRepositoryAdapter
    extends ContentSelectionRepositoryAdapter<YouTubeVideo, StoredYouTubeVideo>
    implements YouTubeVideoSelectionRepository {

  private final StoredContentMapper storedContentMapper;

  public YouTubeVideoSelectionRepositoryAdapter(
      final JdbcTemplate jdbcTemplate, final StoredContentMapper storedContentMapper) {
    super(jdbcTemplate, "youtube", StoredYouTubeVideo.class);
    this.storedContentMapper = storedContentMapper;
  }

  @Override
  protected StoredYouTubeVideo toStored(final YouTubeVideo item) {
    return storedContentMapper.toStored(item);
  }

  @Override
  protected YouTubeVideo toDomain(final StoredYouTubeVideo stored) {
    return storedContentMapper.toDomain(stored);
  }
}
