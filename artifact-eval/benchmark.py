#!/usr/bin/env python3

import argparse
import csv
import re
import subprocess
import sys
from pathlib import Path

build_dir = Path("build")
reports_dir = Path(build_dir, "reports")


def run(args) -> str:
    """Executes the given shell command and returns stderr."""
    return subprocess.run(args, stderr=subprocess.PIPE, text=True).stderr


def get_make_variable(variable) -> str:
    """Returns the value of the given variable as defined in Makefile."""
    return subprocess.run(["make", f"print-{variable}"], check=True, capture_output=True, text=True).stdout


def write_report(file, rows):
    """Writes the given CSV report to the standard output and to the given file."""
    # Write report to the report file
    with open(file, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(rows)

    # Write report to the standard output
    writer = csv.writer(sys.stdout)
    writer.writerows(rows)


def rq2(args):
    research_question = args.COMMAND
    rq_build_dir = Path(build_dir, research_question)
    report_file = Path(reports_dir, f"{research_question}.csv")

    # Run the benchmarks
    benchmarks = [Path(bench).stem for bench in get_make_variable("ANNOTATED_BENCHMARKS").split()]
    build_log = run(["make", f"BUILD_DIR={rq_build_dir}", "clean", "lan"])

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
    print("RQ3")


def argument_parser():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="COMMAND", required=True)

    rq2_parser = subparsers.add_parser("rq2")
    rq2_parser.set_defaults(func=rq2)

    rq3_parser = subparsers.add_parser("rq3")
    rq3_parser.set_defaults(func=rq3)

    return parser


if __name__ == "__main__":
    arguments = argument_parser().parse_args()
    Path(reports_dir).mkdir(parents=True, exist_ok=True)
    arguments.func(arguments)
