package com.athlete.atlas.output.persistence;

import com.athlete.atlas.model.instagram.InstagramPost;
import com.athlete.atlas.port.output.InstagramPostSelectionRepository;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryInstagramPostSelectionRepository extends InMemorySelectionRepository<InstagramPost>
    implements InstagramPostSelectionRepository {}
