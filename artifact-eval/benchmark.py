#!/usr/bin/env python3

import argparse
import csv
import functools
import re
import subprocess
import sys
from enum import Enum, auto
from glob import glob
from os import PathLike
from pathlib import Path
from typing import Mapping

build_dir = Path("build")


def display_command(command, file=sys.stderr):
    """Displays a list of arguments."""
    print(" ".join([str(arg) for arg in command]), file=file)


def make(args, build_directory=build_dir) -> str:
    """Executes make with the given arguments and returns stderr."""
    command = ["make", f"BUILD_DIR={build_directory}"] + args
    display_command(command)
    return subprocess.run(command,
                          check=True,
                          stdout=sys.stderr, stderr=subprocess.PIPE,
                          text=True, encoding="utf-8").stderr


def get_make_variable(variable, build_directory=build_dir) -> str:
    """Returns the value of the given variable as defined in Makefile."""
    return subprocess.run(
        ["make", f"BUILD_DIR={build_directory}", f"print-{variable}"],
        check=True,
        capture_output=True, text=True,
        encoding="utf-8").stdout.strip()


@functools.lru_cache()
def viaduct_command():
    """Returns a command name for running the Viaduct compiler."""
    return get_make_variable("VIADUCT")


def viaduct_run(program: PathLike, host_inputs: Mapping[str, PathLike], host_logs: Mapping[str, PathLike]):
    """Runs the given compiled program for all hosts, writing the stderr of each host to the specified file."""

    # Spin up a process for each host
    host_processes = {}
    for host, host_input in sorted(host_inputs.items()):
        command = [viaduct_command(), "-v", "run", host, "--input", host_input, program]
        display_command(command)

        host_log = Path(host_logs[host])
        host_log.parent.mkdir(parents=True, exist_ok=True)
        with open(host_log, 'w') as f:
            host_processes[host] = subprocess.Popen(command, stdout=subprocess.DEVNULL, stderr=f,
                                                    text=True, encoding="utf-8")

    # Wait for host processes to terminate and receive their output
    for host, host_process in sorted(host_processes.items()):
        host_process.wait()
        if host_process.returncode != 0:
            print(f"ERROR: process for {host} failed. See {host_logs[host]}", file=sys.stderr)
            exit(1)


def detect_host_inputs(benchmark) -> Mapping[str, PathLike]:
    """Computes host files for the given benchmark by analyzing the file system."""
    inputs_directory = "inputs"
    input_files = glob(f"{inputs_directory}/{benchmark}-*.txt")
    if not input_files:
        return {"alice": Path(inputs_directory, "alice.txt"), "bob": Path(inputs_directory, "bob.txt")}
    else:
        host_inputs = {}
        for input_file in input_files:
            host = re.search(r".*-(\w*)\.txt", input_file).group(1)
            host_inputs[host] = Path(input_file)
        return host_inputs


class CompilationStrategy(Enum):
    BOOL = auto()
    YAO = auto()
    OPT_LAN = auto()
    OPT_WAN = auto()


def write_report(file, rows):
    """Renders the given rows as CSV and writes them to the standard output and to the given file."""
    # Write report to file
    Path(file).parent.mkdir(parents=True, exist_ok=True)
    with open(file, 'w', newline='') as f:
        csv.writer(f).writerows(rows)

    # Write report to the standard output
    csv.writer(sys.stdout).writerows(rows)

    print("Report written to", file, file=sys.stderr)


def write_log(file, log):
    """Writes the given log to the given file."""
    path = Path(file)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(log)


def rq2(args):
    rq_build_dir = Path(build_dir, args.COMMAND)
    report_file = Path(rq_build_dir, f"report.csv")

    # Run the benchmarks
    benchmarks = [Path(bench).stem for bench in get_make_variable("ANNOTATED_BENCHMARKS").split()]
    build_log = make(["clean", "lan"], rq_build_dir)

    write_log(Path(rq_build_dir, "log", "build.log"), build_log)

    # Parse the build output
    rows = [[
        "Benchmark",
        "Information Flow Variables",
        "Information Flow Time (ms)",
        "Selection Variables",
        "Selection Time (ms)"
    ]]
    information_flow_variables_parser = re.finditer(r"number of label variables: (\d+)", build_log)
    information_flow_time_parser = re.finditer(r"finished information flow analysis, ran for (\d+)ms", build_log)
    selection_variables_parser = re.finditer(r"number of symvars: (\d+)", build_log)
    selection_time_parser = re.finditer(r"finished protocol selection, ran for (\d+)ms", build_log)

    for benchmark in benchmarks:
        if_vars = next(information_flow_variables_parser).group(1)
        if_time = next(information_flow_time_parser).group(1)
        selection_vars = next(selection_variables_parser).group(1)
        selection_time = next(selection_time_parser).group(1)
        row = [benchmark, if_vars, if_time, selection_vars, selection_time]
        rows.append(row)

    write_report(report_file, rows)


def rq3(args):
    rq_build_dir = Path(build_dir, args.COMMAND)
    report_file = Path(rq_build_dir, "report.csv")

    def compiled_file(benchmark, compilation_strategy):
        """Returns the path to the compiled file for the given benchmark."""
        if compilation_strategy is CompilationStrategy.BOOL:
            return Path("compiled", f"{benchmark}Bool.via")
        elif compilation_strategy is CompilationStrategy.YAO:
            return Path("compiled", f"{benchmark}Yao.via")
        elif compilation_strategy is CompilationStrategy.OPT_LAN:
            return Path(rq_build_dir, "lan", f"{benchmark}.via")
        elif compilation_strategy is CompilationStrategy.OPT_WAN:
            return Path(rq_build_dir, "wan", f"{benchmark}.via")

    benchmarks = [
        "Biomatch",
        "HhiScore",
        "HistoricalMillionaires",
        "Kmeans",
        "Median",
        "TwoRoundBidding",
    ]

    # Compile benchmarks
    for compilation_strategy in [CompilationStrategy.OPT_LAN, CompilationStrategy.OPT_WAN]:
        for benchmark in benchmarks:
            make([compiled_file(benchmark, compilation_strategy)], build_directory=rq_build_dir)

    # Run benchmarks and gather data
    table_header = [
        "Benchmark",
        "Variant",
        "Iteration",
        "Host",
        "Running Time (s)",
        "Communication (MB)"
    ]
    raw_rows = [table_header]

    def parse_row_data(host_log):
        with open(host_log) as f:
            aby_sent_bytes = 0
            aby_received_bytes = 0
            runtime_sent_bytes = None
            runtime_received_bytes = None
            total_running_time = None

            for line in f:
                match = re.search(r"total sent/recv: (?P<sent>\d+) / (?P<received>\d+)", line)
                if match:
                    aby_sent_bytes += int(match.group("sent"))
                    aby_received_bytes += int(match.group("received"))

                match = re.search(r"bytes received from host \w+: (\d+)", line)
                if match:
                    runtime_sent_bytes = int(match.group(1))

                match = re.search(r"bytes sent to host \w+: (\d+)", line)
                if match:
                    runtime_received_bytes = int(match.group(1))

                match = re.search(r"finished interpretation, total running time: (\d+)ms", line)
                if match:
                    total_running_time = int(match.group(1))

            total_sent_bytes = aby_sent_bytes + runtime_sent_bytes
            total_received_bytes = aby_received_bytes + runtime_received_bytes

            return [total_running_time / 1000.0, (total_sent_bytes + total_received_bytes) / 1024.0 / 1024.0]

    for benchmark in benchmarks:
        host_inputs = detect_host_inputs(benchmark)
        for compilation_strategy in CompilationStrategy:
            for iteration in range(1, args.iterations + 1):
                print(f"Running {benchmark}/{compilation_strategy.name} ({iteration})",
                      file=sys.stderr)
                print("Inputs:", file=sys.stderr)
                for host, host_input in sorted(host_inputs.items()):
                    print(f"  {host}: {host_input}", file=sys.stderr)

                host_logs = {host: Path(rq_build_dir, "log", compilation_strategy.name.lower(),
                                        f"{benchmark}-{host}-{iteration}.log")
                             for host in host_inputs
                             }

                viaduct_run(compiled_file(benchmark, compilation_strategy), host_inputs, host_logs)

                csv.writer(sys.stderr).writerow(table_header)
                for host, host_logfile in sorted(host_logs.items()):
                    row = [benchmark, compilation_strategy.name, iteration, host] + parse_row_data(host_logfile)
                    csv.writer(sys.stderr).writerow(row)
                    raw_rows.append(row)

    write_report(report_file, raw_rows)


def rq4(args):
    rq_build_dir = Path(build_dir, args.COMMAND)
    report_file = Path(rq_build_dir, "report.txt")

    # Compile benchmark programs
    benchmarks = [Path(bench).stem for bench in get_make_variable("ANNOTATED_BENCHMARKS").split()]
    erased_benchmarks = [Path(bench).stem for bench in get_make_variable("ERASED_BENCHMARKS").split()]

    build_log = make(["lan", "erased"], rq_build_dir)
    write_log(Path(rq_build_dir, "log", "build.log"), build_log)

    rq_bench_build_dir = Path(rq_build_dir, "lan")
    with open(report_file, "w") as f:
        for benchmark in benchmarks:
            erased_benchmark = f"{benchmark}Erased"
            if erased_benchmark not in erased_benchmarks:
                continue

            bench_file = Path(rq_bench_build_dir, f"{benchmark}.via")
            erased_bench_file = Path(rq_bench_build_dir, f"{erased_benchmark}.via")

            command = ["diff", bench_file, erased_bench_file]
            display_command(command)
            display_command(command, f)
            bench_diff = subprocess.run(command, stdout=subprocess.PIPE, text=True, encoding="utf-8").stdout
            f.write(f"{bench_diff}\n\n")

    print("Report written to", report_file, file=sys.stderr)


def argument_parser():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="COMMAND", required=True)

    rq2_parser = subparsers.add_parser("rq2", help="benchmark compilation time")
    rq2_parser.set_defaults(func=rq2)

    rq3_parser = subparsers.add_parser("rq3", help="benchmark execution time")
    rq3_parser.set_defaults(func=rq3)
    rq3_parser.add_argument("-i", "--iterations", dest="iterations", type=int, default=1,
                            help="number of times to run each benchmark")

    rq4_parser = subparsers.add_parser("rq4", help="benchmark annotation burden")
    rq4_parser.set_defaults(func=rq4)

    return parser


if __name__ == "__main__":
    arguments = argument_parser().parse_args()
    arguments.func(arguments)
