package me.bechberger.programs;

import me.bechberger.RunConfig;

import java.time.Duration;
import java.time.Instant;

public record Result(RunConfig config, String event, int success, int error, Duration runtime) {

    public float successRate() {
        return (float) success / (success + error);
    }

    public float eventsPerSecond() {
        return (float) (success + error) / (runtime.getSeconds() + runtime.getNano() / 1_000_000_000f);
    }
}
