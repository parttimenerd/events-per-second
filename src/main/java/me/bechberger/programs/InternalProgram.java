package me.bechberger.programs;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.consumer.RecordingStream;
import me.bechberger.RunConfig;
import me.bechberger.Store;

import javax.security.auth.login.Configuration;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A program that directly runs on the JVM
 */
public abstract class InternalProgram implements Program {

    private static class SuccessAndError {
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger error = new AtomicInteger(0);
    }

    @Name("Stop")
    static class StopEvent extends Event {
    }

    private final AtomicBoolean stop = new AtomicBoolean(false);

    @Override
    public List<Result> run(RunConfig config, Duration duration, Duration printEvery) {
        System.out.println("Running " + config + " for " + duration);
        stop.set(false);
        AtomicReference<Instant> start = new AtomicReference<>();
        Instant end;
        Map<String, SuccessAndError> eventMap = new HashMap<>();
        try (var recording = new RecordingStream()) {
            recording.setSettings(config.toJFRSettings());
            for (String event : config.events()) {
                eventMap.put(event, new SuccessAndError());
                recording.onEvent(event, value -> {
                    if (value.getStackTrace() != null && !value.getStackTrace().getFrames().isEmpty()) {
                        eventMap.get(event).success.incrementAndGet();
                    } else {
                        eventMap.get(event).error.incrementAndGet();
                    }
                });
                recording.onEvent("Stop", value -> {
                    System.out.println("Stopping recording");
                    stop.set(true);
                });
            }
            var logThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(printEvery);
                        Duration currentDuration = Duration.between(start.get(), Instant.now());
                        float currentDurationSeconds = currentDuration.getSeconds() + currentDuration.getNano() / 1_000_000_000f;
                        for (var entry : eventMap.entrySet()) {
                            int error = entry.getValue().error.get();
                            int success = entry.getValue().success.get();
                            if (error == 0 && success == 0) {
                                continue;
                            }
                            // print name, success and error per second
                            System.out.printf("%40s: %5d success",
                                    entry.getKey(),
                                    success
                                    );
                            if (entry.getValue().success.get() > 0) {
                                System.out.printf(", %.4f events/s", (float) (success + error) / currentDurationSeconds);
                            }
                            if (entry.getValue().error.get() > 0) {
                                System.out.printf(", %.4f errors/s, %.4f success rate",
                                        (float) error / currentDurationSeconds,
                                        (float) success / (success + error * 1.0f));
                            }
                            System.out.println();
                        }

                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            start.set(Instant.now());
            logThread.start();
            recording.startAsync();

            runThreads(duration, config.cores());
            end = Instant.now();
            recording.stop();
            logThread.interrupt();
            System.out.println("Recording stopped");
        }
        return config.events().stream().map(event -> new Result(config, event, eventMap.get(event).success.get(), eventMap.get(event).error.get(), Duration.between(start.get(), end))).toList();
    }

    void runThreads(Duration duration, int cores) {
        Thread[] threads = new Thread[cores];
        for (int i = 0; i < cores; i++) {
            threads[i] = new Thread(() -> run(duration));
            threads[i].start();
        }
        for (int i = 0; i < cores; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    abstract void run(Duration duration);

}
