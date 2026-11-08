package org.freakz.engine.services.water;

import java.time.Instant;

/** Retrieval time belongs to the processed reading and never changes on cache hits. */
public record WaterTemperatureReading(String station, String stationUrl, double temperatureC, Instant retrievedAt) {}
