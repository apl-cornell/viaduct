#!/usr/bin/env python3

import re
import sys

TRIAL_START = r"starting trial (?P<trial>\d+)"
CASE_START = r"executing (?P<case>\S+)"
ABY_BYTES = r"total sent/recv: (?P<sent>\d+) / (?P<recv>\d+)"
RUN_TIME = r"Interpreter - finished interpretation, total running time: (?P<ms>\d+)ms"
RUNTIME_SENT = r"Runtime - bytes sent to host \w+: (?P<bytes>\d+)"
RUNTIME_RECV = r"Runtime - bytes received from host \w+: (?P<bytes>\d+)"


def parse_data(filename):
    trial = None
    case = None
    data = {}

    # parse data from log
    with open(filename) as f:
        for line in f.readlines():
            trial_match = re.search(TRIAL_START, line)
            case_match = re.search(CASE_START, line)
            aby_bytes_match = re.search(ABY_BYTES, line)
            run_time_match = re.search(RUN_TIME, line)
            runtime_sent_match = re.search(RUNTIME_SENT, line)
            runtime_recv_match = re.search(RUNTIME_RECV, line)

            if trial_match is not None:
                trial = int(trial_match.group("trial"))
                data[trial] = {}

            elif case_match is not None:
                case = case_match.group("case")
                data[trial][case] = {"sent": 0, "recv": 0, "time": 0}

            elif aby_bytes_match is not None:
                data[trial][case]["sent"] += int(aby_bytes_match.group("sent"))
                data[trial][case]["recv"] += int(aby_bytes_match.group("recv"))

            elif run_time_match is not None:
                data[trial][case]["time"] += int(run_time_match.group("ms"))

            elif runtime_sent_match is not None:
                data[trial][case]["sent"] += int(runtime_sent_match.group("bytes"))

            elif runtime_recv_match is not None:
                data[trial][case]["recv"] += int(runtime_recv_match.group("bytes"))

    return data


def merge_data(data1, data2):
    data = {}

    for trial, trial_data in data1.items():
        data[trial] = {}

        for case, case_data in trial_data.items():
            data[trial][case] = {}
            data[trial][case]["time"] = max(case_data["time"], data2[trial][case]["time"])
            data[trial][case]["sent"] = max(case_data["sent"], data2[trial][case]["recv"])
            data[trial][case]["recv"] = max(case_data["recv"], data2[trial][case]["sent"])

    return data


def get_average(data):
    num_trials = len(data.items())
    avg_data = {}
    for trial, trial_data in data.items():
        for case, case_data in trial_data.items():
            if case not in avg_data:
                avg_data[case] = {"time": 0, "comm": 0}

            avg_data[case]["time"] += case_data["time"]
            avg_data[case]["comm"] += case_data["sent"] + case_data["recv"]

    for case in avg_data.keys():
        avg_data[case]["time"] /= num_trials
        avg_data[case]["comm"] /= num_trials

    return avg_data


def main(argv):
    if len(argv) <= 1:
        print("usage: ./parseexec [EXEC_FILE]")
        exit(1)

    data1 = parse_data(argv[1])
    data2 = parse_data(argv[2])
    data = merge_data(data1, data2)
    avg_data = get_average(data)

    for case, case_data in avg_data.items():
        print(case)
        time = round(float(case_data["time"]) / 1000.0, 1)
        comm = round(float(case_data["comm"]) / 1000.0, 1)
        print("time: {0:>8,} s; comm: {1:>8,} KiB".format(time, comm))


if __name__ == "__main__":
    main(sys.argv)
