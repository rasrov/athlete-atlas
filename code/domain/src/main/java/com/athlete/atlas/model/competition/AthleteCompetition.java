package com.athlete.atlas.model.competition;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URI;
import java.time.LocalDate;

public record AthleteCompetition(@Nonnull String id, @Nonnull String name, @Nonnull LocalDate startDate, @Nonnull LocalDate endDate, @Nullable String city, @Nullable String country, @Nonnull String description, @Nonnull
                                 URI sourceUrl, @Nonnull String sourceId, @Nullable URI logoUrl) {}
