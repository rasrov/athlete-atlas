package com.athlete.atlas.usecase;

import com.athlete.atlas.config.InstagramSelectionProperties;
import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.model.selection.AthleteSelection;
import com.athlete.atlas.port.input.instagram.AthleteInstagramPostUseCase;
import com.athlete.atlas.port.input.instagram.RefreshInstagramPostsUseCase;
import com.athlete.atlas.port.output.InstagramPort;
import com.athlete.atlas.port.output.InstagramPostSelectionRepository;
import com.athlete.atlas.usecase.selection.SynchronizedSelectionService;
import jakarta.annotation.Nonnull;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Latest Instagram posts of each athlete, refreshed periodically from their profile. */
@Component
public class AthleteInstagramPostService extends SynchronizedSelectionService<InstagramPost>
    implements AthleteInstagramPostUseCase, RefreshInstagramPostsUseCase {

  private final InstagramPort instagramPort;

  private final InstagramSelectionProperties instagramSelectionProperties;

  public AthleteInstagramPostService(
      final InstagramPort instagramPort,
      final InstagramPostSelectionRepository instagramPostSelectionRepository,
      final InstagramSelectionProperties instagramSelectionProperties,
      final Clock clock) {
    super("Instagram", instagramPostSelectionRepository, clock);
    this.instagramPort = instagramPort;
    this.instagramSelectionProperties = instagramSelectionProperties;
  }

  @Override
  public AthleteSelection<InstagramPost> listAthleteInstagramPosts(
      @Nonnull final String athleteId) {
    return getSelection(athleteId);
  }

  @Override
  protected Map<String, String> accounts() {
    return instagramSelectionProperties.usernames();
  }

  @Override
  protected Duration freshnessThreshold() {
    return instagramSelectionProperties.freshnessThreshold();
  }

  @Override
  protected List<InstagramPost> fetch(final String username) {
    return instagramPort.fetchLatestPosts(username);
  }
}
