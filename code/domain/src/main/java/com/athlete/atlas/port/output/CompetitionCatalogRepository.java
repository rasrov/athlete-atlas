package com.athlete.atlas.port.output;

import com.athlete.atlas.model.competition.CompetitionCatalog;
import jakarta.annotation.Nonnull;
import java.util.Optional;

/** Stores the last competition catalog. */
public interface CompetitionCatalogRepository {

  Optional<CompetitionCatalog> find();

  /** Replaces the whole catalog. */
  void save(@Nonnull CompetitionCatalog catalog);
}
