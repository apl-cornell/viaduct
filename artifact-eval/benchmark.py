#!/usr/bin/env python3

import argparse
import csv
import functools
import re
import subprocess
import sys
from enum import Enum, auto
from os import PathLike
from pathlib import Path
from typing import Mapping

build_dir = Path("build")


def display_command(command):
    """Displays a list of arguments."""
    print(" ".join([str(arg) for arg in command]), file=sys.stderr)


def make(args, build_directory=build_dir) -> str:
    """Executes make with the given arguments and returns stderr."""
    command = ["make", f"BUILD_DIR={build_directory}"] + args
    display_command(command)
    return subprocess.run(command, stdout=sys.stderr, stderr=subprocess.PIPE, text=True, encoding="utf-8").stderr


def get_make_variable(variable, build_directory=build_dir) -> str:
    """Returns the value of the given variable as defined in Makefile."""
    return subprocess.run(
        ["make", f"BUILD_DIR={build_directory}", f"print-{variable}"],
        check=True,
        capture_output=True, text=True,
        encoding="utf-8").stdout.strip()


@functools.cache
def viaduct_command():
    """Returns a command name for running the Viaduct compiler."""
    return get_make_variable("VIADUCT")


def viaduct_run(program: PathLike, host_inputs: Mapping[str, PathLike]):
    """Runs the given compiled program for all hosts, and returns the stderr for each host."""

    # Spin up a process for each host
    host_processes = {}
    for host, host_input in host_inputs.items():
        command = [viaduct_command(), "-v", "run", host, "--input", host_input, program]
        display_command(command)
        host_processes[host] = subprocess.Popen(command, stdout=sys.stderr, stderr=subprocess.PIPE, text=True,
                                                encoding="utf-8")

    # Wait for host processes to terminate and receive their output
    host_logs = {}
    for host, host_process in host_processes.items():
        _, stderr = host_process.communicate()
        host_logs[host] = stderr

    return host_logs


class CompilationStrategy(Enum):
    BOOL = auto()
    YAO = auto()
    OPT_LAN = auto()
    OPT_WAN = auto()


def write_report(file, rows):
    """Renders the given rows as CSV and writes them to the standard output and to the given file."""
    # Write report to file
    with open(file, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(rows)

    # Write report to the standard output
    writer = csv.writer(sys.stdout)
    writer.writerows(rows)

    print("Report written to", file, file=sys.stderr)


def rq2(args):
    rq_build_dir = Path(build_dir, args.COMMAND)
    report_file = Path(rq_build_dir, f"report.csv")

    # Run the benchmarks
    benchmarks = [Path(bench).stem for bench in get_make_variable("ANNOTATED_BENCHMARKS").split()]
    build_log = make(["clean", "lan"], rq_build_dir)

    # Parse the build output
    rows = [
        ["Benchmark",
         "Information Flow Variables",
         "Information Flow Time (ms)",
         "Selection Variables",
         "Selection Time (ms)"]]
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
    report_file = Path(rq_build_dir, f"report.csv")

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

    # Run benchmarks
    host_inputs = {"alice": Path("alice-input.txt"), "bob": Path("bob-input.txt")}

    for benchmark in benchmarks:
        for compilation_strategy in CompilationStrategy:
            print(f"Running {benchmark}/{compilation_strategy.name}", file=sys.stderr)
            host_logs = viaduct_run(compiled_file(benchmark, compilation_strategy), host_inputs)

            # Write execution logs to disk
            for host, host_log in host_logs.items():
                log_file = Path(rq_build_dir, "log", compilation_strategy.name.lower(), f"{benchmark}-{host}.log")
                log_file.parent.mkdir(parents=True, exist_ok=True)
                log_file.write_text(host_log)


def rq4(args):
    print("RQ4: TODO")


def argument_parser():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="COMMAND", required=True)

    rq2_parser = subparsers.add_parser("rq2", help="benchmark compilation time")
    rq2_parser.set_defaults(func=rq2)

    rq3_parser = subparsers.add_parser("rq3", help="benchmark execution time")
    rq3_parser.set_defaults(func=rq3)
    rq3_parser.add_argument("-i", "--iterations", dest="iterations", type=int, default=5,
                            help="number of times to run each benchmark")

    rq4_parser = subparsers.add_parser("rq4", help="benchmark annotation burden")
    rq4_parser.set_defaults(func=rq4)

    return parser


if __name__ == "__main__":
    arguments = argument_parser().parse_args()
    arguments.func(arguments)
