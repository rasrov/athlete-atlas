package com.athlete.atlas.usecase;

import com.athlete.atlas.exception.StorageUnavailableException;
import com.athlete.atlas.model.athlete.Athlete;
import com.athlete.atlas.port.output.AthleteRepository;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Athlete storage of the tests, which can be switched off to simulate an unavailable database. */
final class FakeAthleteRepository implements AthleteRepository {

  private final List<Athlete> athletes = new ArrayList<>();

  private boolean available = true;

  FakeAthleteRepository(final Athlete... athletes) {
    this.athletes.addAll(List.of(athletes));
  }

  void replaceAll(final Athlete... athletes) {
    this.athletes.clear();
    this.athletes.addAll(List.of(athletes));
  }

  void setAvailable(final boolean available) {
    this.available = available;
  }

  @Override
  public Optional<Athlete> findByCode(@Nonnull final String code) {
    checkAvailable();
    return athletes.stream().filter(athlete -> athlete.code().equals(code)).findFirst();
  }

  @Override
  public List<Athlete> findAll() {
    checkAvailable();
    return List.copyOf(athletes);
  }

  private void checkAvailable() {
    if (!available) {
      throw new StorageUnavailableException("Database unavailable", null);
    }
  }
}
