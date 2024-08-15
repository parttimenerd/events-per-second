package me.bechberger.programs;

import me.bechberger.RunConfig;
import me.bechberger.Store;

import java.time.Duration;
import java.util.List;

public interface Program {

    List<Result> run(RunConfig config, Duration duration, Duration printEvery);

}
