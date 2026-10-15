package com.athlete.atlas.output.persistence.content;

import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.port.output.InstagramPostSelectionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InstagramPostSelectionRepositoryAdapter
    extends ContentSelectionRepositoryAdapter<InstagramPost, StoredInstagramPost>
    implements InstagramPostSelectionRepository {

  private final StoredContentMapper storedContentMapper;

  public InstagramPostSelectionRepositoryAdapter(
      final JdbcTemplate jdbcTemplate, final StoredContentMapper storedContentMapper) {
    super(jdbcTemplate, "instagram", StoredInstagramPost.class);
    this.storedContentMapper = storedContentMapper;
  }

  @Override
  protected StoredInstagramPost toStored(final InstagramPost item) {
    return storedContentMapper.toStored(item);
  }

  @Override
  protected InstagramPost toDomain(final StoredInstagramPost stored) {
    return storedContentMapper.toDomain(stored);
  }
}
