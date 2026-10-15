package com.athlete.atlas.output.persistence.content;

import java.net.URI;
import java.time.LocalDate;

/**
 * Stored form of a competition, decoupled from the domain record so that renaming a domain field
 * does not break the stored data.
 */
public record StoredCompetition(
    String id,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    String city,
    String country,
    String description,
    URI sourceUrl,
    String sourceId,
    URI logoUrl) {}
