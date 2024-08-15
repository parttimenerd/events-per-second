package me.bechberger;

import me.bechberger.programs.Result;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps {@link RunConfig} to the number of every event (both success and error) and the runtime
 */
public class Store {

    private final Map<String, List<Result>> results;

    public Store() {
        this.results = new HashMap<>();
    }

    public void add(List<Result> newResults) {
        for (Result result : newResults) {
            results.computeIfAbsent(result.event(), k -> new ArrayList<>()).add(result);
        }
    }

    public String toCSV(Duration period) {
        StringBuilder builder = new StringBuilder();
        builder.append("Event,Cores,Success,Error,Success Rate,Events per Second,Runtime\n");
        for (Map.Entry<String, List<Result>> entry : results.entrySet()) {
            for (Result result : entry.getValue()) {
                if (!result.config().period().equals(period)) {
                    continue;
                }
                builder.append(String.format("%s,%d,%d,%d,%.3f,%.2f,%.2f\n", result.event(), result.config().cores(), result.success(),
                        result.error(), result.successRate(), result.eventsPerSecond(),
                        result.runtime().toMillis() / 1_000f));
            }
        }
        return builder.toString();
    }
}