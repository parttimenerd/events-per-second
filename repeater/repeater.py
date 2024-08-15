#!/usr/bin/env python3
import subprocess
import threading
import os
from queue import Queue
import time
import argparse
import re
import atomicx
from atomicx.atomicx import AtomicInt


# Function to execute the command
def run_command(command, check_pattern, success: AtomicInt, err_code_fail: AtomicInt, regex_fail: AtomicInt):
    while True:
        try:
            # Run the shell command
            result = subprocess.run(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout_output = result.stdout.decode('utf-8')
            stderr_output = result.stderr.decode('utf-8')

            # Check the multi-line output against the regular expression
            if check_pattern and not re.search(check_pattern, stdout_output, re.MULTILINE):
                regex_fail.inc()
                print("--- Output failed the regex check ---")
                print(stdout_output)
            elif result.returncode == 0:
                success.inc()
            else:
                err_code_fail.inc()

                # Extract the hs_errpid log file name from stderr
                hs_err_match = re.search(r'hs_err_pid\d+\.log', stderr_output)

                if hs_err_match:
                    hs_err_file = hs_err_match.group(0)
                    if os.path.exists(hs_err_file):
                        with open(hs_err_file, 'r') as log_file:
                            print(f"--- First 30 lines of {hs_err_file} ---")
                            for i, line in enumerate(log_file):
                                if i >= 30:
                                    break
                                print(line.strip())
                    else:
                        print(f"Error log file {hs_err_file} not found.")
                else:
                    print("No hs_errpid log file found in stderr output.")

        except Exception as e:
            err_code_fail.inc()
            print(f"Exception encountered: {e}")


# Function to print the results
def print_results(success: AtomicInt, err_code_fail: AtomicInt, regex_fail: AtomicInt):
    prev_values = (success.load(), err_code_fail.load(), regex_fail.load())

    while True:
        current_values = (success.load(), err_code_fail.load(), regex_fail.load())

        if current_values != prev_values:
            print(f"Success: {current_values[0]:>5}, Err Code Fail: {current_values[1]:>5}, Regex Fail: {current_values[2]:>5}")
            prev_values = current_values

        time.sleep(1)

# Main function to handle threading and command-line arguments
def main():
    parser = argparse.ArgumentParser(
        description="Run a shell command in parallel with specified threads. "
                    "The script tracks the number of successful, failed, and "
                    "regex check-failed runs, printing the results periodically. "
                    "For unsuccessful runs, it outputs the first 30 lines of "
                    "the `hs_errpid<pid>.log` file."
    )

    parser.add_argument('command', type=str, help='The shell command to run.')
    parser.add_argument('threads', type=int, help='Number of threads to run the command in parallel.')
    parser.add_argument('--check-regex', type=str, help='A multiline regular expression to check the command output.')

    args = parser.parse_args()

    success: AtomicInt = AtomicInt(0)
    err_code_fail: AtomicInt = AtomicInt(0)
    regex_fail: AtomicInt = AtomicInt(0)

    # Start the threads
    threads = []
    for _ in range(args.threads):
        thread = threading.Thread(target=run_command, args=(args.command, args.check_regex, success, err_code_fail, regex_fail))
        thread.daemon = True
        threads.append(thread)
        thread.start()

    # Start the results printing thread
    print_thread = threading.Thread(target=print_results, args=(success, err_code_fail, regex_fail))
    print_thread.daemon = True
    print_thread.start()

    # Join the threads (infinite loop, so this won't actually terminate)
    for thread in threads:
        thread.join()


if __name__ == "__main__":
    main()
