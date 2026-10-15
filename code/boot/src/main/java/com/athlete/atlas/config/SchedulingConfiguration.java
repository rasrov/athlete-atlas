package com.athlete.atlas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the periodic refreshes of the application layer (competitions, YouTube, Instagram). */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {}
