package com.athlete.atlas.output.persistence;

import com.athlete.atlas.model.youtube.YouTubeVideoSelection;
import com.athlete.atlas.port.output.YouTubeVideoSelectionRepository;
import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory storage until the backend has a database: selections are lost on restart and rebuilt
 * by the refresh on startup. Each instance keeps its own selections.
 */
@Repository
public class InMemoryYouTubeVideoSelectionRepository implements YouTubeVideoSelectionRepository {

  private final Map<String, YouTubeVideoSelection> selections = new ConcurrentHashMap<>();

  @Override
  public Optional<YouTubeVideoSelection> findByAthleteId(@Nonnull final String athleteId) {
    return Optional.ofNullable(selections.get(athleteId));
  }

  @Override
  public void save(@Nonnull final String athleteId, @Nonnull final YouTubeVideoSelection selection) {
    selections.put(athleteId, selection);
  }
}
