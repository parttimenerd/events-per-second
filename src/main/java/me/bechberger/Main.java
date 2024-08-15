package me.bechberger;

import java.time.Duration;
import java.util.List;
import picocli.CommandLine;

import static picocli.CommandLine.*;

@Command(name = "events", mixinStandardHelpOptions = true, version = "1.0",
        description = "Compute the maximum number of events that JFR generates for CPU-bound application saturating a given number of cores.",
        showDefaultValues = true)
public class Main implements Runnable {

    @Parameters(arity = "0..*", description = "Event names")
    private List<String> eventNames;

    @Option(names = {"--type"}, description = "Type of event", defaultValue = "CPU")
    private ProgramType type;

    @Option(names = {"--cores"}, description = "Number of cores", split = ",")
    private List<Integer> cores = List.of(Runtime.getRuntime().availableProcessors() - 2);

    @Option(names = {"--period"}, description = "Period in ms", split = ",", defaultValue = "10")
    private List<Integer> periods;

    @Option(names = {"--every"}, description = "Print event table every n seconds", defaultValue = "10")
    private int every;

    @Option(names = {"--max-time"}, description = "Max time per combination in seconds", defaultValue = "100")
    private int maxTime;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        if (eventNames == null || eventNames.isEmpty()) {
            CommandLine.usage(this, System.out);
            return;
        }
        Store store = new Store();
        for (RunConfig config : RunConfig.getCombinations(eventNames, type, cores, periods.stream().map(Duration::ofMillis).toList())) {
            store.add(config.run(Duration.ofSeconds(maxTime), Duration.ofSeconds(every)));
            System.out.println("Period: " + config.period());
            System.out.println(store.toCSV(config.period()));
        }
        for (int period : periods) {
            System.out.println("Period: " + period);
            System.out.println(store.toCSV(Duration.ofMillis(period)));
        }
    }

}