import jdk.jfr.consumer.RecordingStream;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Can be used with the {@code repeater/repeater.py} to check that two recordings
 * can be started directly after each other in the same JVM and still produce events.
 */
public class JFRRepeatTest {

    public static void main(String[] args) throws InterruptedException {
        // first argument is duration in seconds or 1 if not provided
        Duration maxRuntime = args.length > 0 ? Duration.ofSeconds(Integer.parseInt(args[0])) : Duration.ofSeconds(1);
        var loadThread = new Thread(() -> {
            int result = 0;
            while (true) {
                for (int i = 0; i < 1000; i++) {
                    for (int j = 0; j < 1000; j++) {
                        result += (i * j) ^ (i + j) % (i + 1);
                        result -= (i - j) * (j + 1) / (i + 1);
                        result ^= (i * 3) + (j * 2) - (i / 2);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        loadThread.start();
        System.out.println("First run:");
        run(maxRuntime);
        System.out.println("\n");
        System.out.println("Second run:");
        run(maxRuntime);
        loadThread.interrupt();
        System.exit(0);
    }

    static void run(Duration maxRuntime) {
        AtomicBoolean gotCPUEvent = new AtomicBoolean(false);
        AtomicBoolean gotEvent = new AtomicBoolean(false);
        try (var stream = new RecordingStream()) {
            stream.enable("jdk.ExecutionSample").with("period", "100ms");
            stream.onEvent("jdk.ExecutionSample", event -> {
                if (!gotEvent.get()) {
                    System.out.println("  Got event");
                }
                gotEvent.set(true);
            });
            stream.enable("jdk.CPUTimeExecutionSample").with("period", "100ms");
            stream.onEvent("jdk.CPUTimeExecutionSample", event -> {
                if (!gotCPUEvent.get()) {
                    System.out.println("  Got CPU event");
                }
                gotCPUEvent.set(true);
            });
            stream.startAsync();
            // while not gotEvent and < 1000ms
            Instant start = Instant.now();
            while (!gotEvent.get() && Duration.between(start, Instant.now()).compareTo(maxRuntime) < 0) {
                Thread.onSpinWait(); // this has to be a busy loop, otherwise the event might not be generated
            }
            stream.stop();
            if (!gotCPUEvent.get()) {
                System.out.println("  No CPU event received");
                System.exit(1);
            }
        }
    }
}
