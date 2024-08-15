Repeater
--------

Small python tool to run files like
[../src/main/java/JFRRepeatTest.java] repeatedly 
and check for hs_err files and the return code.

Usage
-----

You can run the script as follows:

```bash
python parallel_command.py "<your_command_here>" <number_of_threads> --check-regex "<your_multiline_regex>"

    <your_command_here>: The shell command you want to run in parallel.
    <number_of_threads>: The number of parallel threads you want to run.
    --check-regex "<your_multiline_regex>": (Optional) A multi-line regular expression to validate the output of the command.
```

Example:
    
```bash
python3 parallel_command.py "java ../src/main/java/JFRRepeatTest.java" 4 --check-regex '(Got CPU event(.*\n)+.*Got CPU event)'```

License
-------
MIT, Copyright 2024 SAP SE or an SAP affiliate company, Johannes Bechberger and contributors