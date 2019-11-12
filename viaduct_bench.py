#!/usr/bin/env python

import json
import os
import subprocess
import sys
import time

VIADUCT_PLAYER = "./viaduct_player.py"
RESULT_FILE_TEMPLATE = "player_{}.json"

def run_tests(test_config):
  timestamp = str(int(time.time()))
  run_result_dir = os.path.join(test_config["result_dir"], timestamp)

  if not os.path.isdir(run_result_dir):
    os.makedirs(run_result_dir)

  for test in test_config["tests"]:
    print "running test", test["name"], "..."

    test_result_dir = os.path.join(run_result_dir, test["name"])

    if not os.path.isdir(test_result_dir):
      os.makedirs(test_result_dir)

    proclist = []
    i = 0
    while i < test["num_players"]:
      prog_dir = os.path.join(test_config["test_dir"], test["name"])

      input_file = os.path.join(prog_dir, test["input_files"][i])

      output_file = os.path.join(test_result_dir, RESULT_FILE_TEMPLATE.format(i))

      cmd = "{} {} {} {} < {}".format( \
          VIADUCT_PLAYER, i, prog_dir, output_file, input_file)

      proc = subprocess.Popen(cmd, shell=True, stdout=None, stdin=None, stderr=None)

      proclist.append(proc)
      i += 1

    for proc in proclist:
      proc.wait()


if sys.argv[1] == "run" and len(sys.argv) == 3:
  test_config_file = sys.argv[2]

  with open(test_config_file) as f:
    contents = f.read()
    config_json = json.loads(contents)
    run_tests(config_json)

elif sys.argv[2] == "collate" and len(sys.argv) == 3:
  print "not implemented yet!"

else:
  print "usage: viaduct_bench.py run test_config_file"
  print "usage: viaduct_bench.py collate results_dir"

