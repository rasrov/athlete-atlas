package com.athlete.atlas.output.persistence;

import com.athlete.atlas.model.competition.CompetitionCatalog;
import com.athlete.atlas.port.output.CompetitionCatalogRepository;
import jakarta.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Repository;

/**
 * In-memory storage until the backend has a database: the catalog is lost on restart and rebuilt
 * by the refresh on startup. Each instance keeps its own catalog. Saving replaces it atomically.
 */
@Repository
public class InMemoryCompetitionCatalogRepository implements CompetitionCatalogRepository {

  private final AtomicReference<CompetitionCatalog> catalog = new AtomicReference<>();

  @Override
  public Optional<CompetitionCatalog> find() {
    return Optional.ofNullable(catalog.get());
  }

  @Override
  public void save(@Nonnull final CompetitionCatalog competitionCatalog) {
    catalog.set(competitionCatalog);
  }
}
