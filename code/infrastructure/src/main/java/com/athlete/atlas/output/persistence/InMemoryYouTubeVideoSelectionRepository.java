package com.athlete.atlas.output.persistence;

import com.athlete.atlas.model.youtube.YouTubeVideo;
import com.athlete.atlas.port.output.YouTubeVideoSelectionRepository;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryYouTubeVideoSelectionRepository extends InMemorySelectionRepository<YouTubeVideo>
    implements YouTubeVideoSelectionRepository {}
