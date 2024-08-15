package me.bechberger;

import me.bechberger.programs.CPUBoundProgram;
import me.bechberger.programs.Result;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RunConfig(List<String> events, ProgramType type, int cores, Duration period) {
    public RunConfig {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events must not be empty");
        }
        if (cores < 1) {
            throw new IllegalArgumentException("Cores must be at least 1");
        }
    }

    public Map<String, String> toJFRSettings() {
        Map<String, String> settings = new HashMap<>();
        for (String event : events) {
            settings.put(event + "#enabled", "" + true);
            settings.put(event + "#period", period.toMillis() + "ms");
        }
        return settings;
    }

    public static List<RunConfig> getCombinations(List<String> eventNames, ProgramType type, List<Integer> cores, List<Duration> periods) {
        List<RunConfig> result = new ArrayList<>();
        for (int core : cores) {
            for (Duration period : periods) {
                result.add(new RunConfig(eventNames, type, core, period));
            }
        }
        return result;
    }

    public List<Result> run(Duration duration, Duration printEvery) {
        switch (type) {
            case CPU:
                return new CPUBoundProgram().run(this, duration, printEvery);
            default:
                throw new IllegalArgumentException(type + " is not yet supported");
        }
    }
}