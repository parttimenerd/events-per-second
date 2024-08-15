package me.bechberger.programs;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class CPUBoundProgram extends InternalProgram {

    @Override
    public void run(Duration duration) {
        Instant start = Instant.now();
        try (FileWriter writer = new FileWriter("/dev/null")) {
            long result = 0;
            while (Duration.between(start, Instant.now()).compareTo(duration) < 0) {
                // counting loop to waste CPU cycles, should not be optimized away

                for (int i = 0; i < 1000; i++) {
                    for (int j = 0; j < 1000; j++) {
                        result += (i * j) ^ (i + j) % (i + 1);
                        result -= (i - j) * (j + 1) / (i + 1);
                        result ^= (i * 3) + (j * 2) - (i / 2);
                    }
                }
            }
            // add side effect to prevent optimization
            writer.write(String.valueOf(result));
        } catch (IOException _) {
        }
    }
}
