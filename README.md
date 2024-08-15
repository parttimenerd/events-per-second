Events per Second
=================

**See [repeater](repeater) for a tool to run recordings in a loop.**

Compute the maximum number of events that JFR generates
for CPU-bound application saturating a given number of cores.

Usage
-----
```bash
mvn package
java -jar target/events.jar <event names> --type <CPU,NATIVE,RENAISSANCE, default IO> --cores <cores, default all cores - 2, multiple values are possible> --period <period in ms, default 10, multiple are possible> --every <print event table every n seconds, default 10> --max-time <max time per combination in secods, default 100>
```

License
-------
MIT, Copyright 2024 SAP SE or an SAP affiliate company, Johannes Bechberger and contributors