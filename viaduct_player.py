#!/usr/bin/env python

from Queue import Queue
import threading
import subprocess
import sys
import os


class MambaOutThread (threading.Thread):
  def __init__(self, mamba_proc, in_queue, prof_data):
    threading.Thread.__init__(self)
    self.mamba_proc = mamba_proc
    self.in_queue = in_queue
    self.prof_data = prof_data

  def get_lines(self):
    for line in iter(self.mamba_proc.stdout.readline, ""):
      yield line

  def run(self):
    for line in self.get_lines():
      if "VIADUCT_OUTPUT" in line:
        self.in_queue.put(int(line.split()[1]), block=False)

      elif "VIADUCT_PROFILE_NETWORK" in line:
        data = line.split()
        player = data[1]
        thread_num = data[2]
        data_sent = int(data[3])
        data_received = int(data[4])

        if player not in prof_data["network"]:
          prof_data["network"][player] = {}

        prof_data["network"][player][thread_num] = {
          "sent": data_sent,
          "received": data_received
        }

      elif "VIADUCT_PROFILE_RESOURCE" in line:
        data = line.split()
        player = data[1]
        thread_num = data[2]
        process_num = data[3]
        maxrss = int(data[4])

        if player not in prof_data["resource"]:
          prof_data["resource"][player] = {}

        prof_data["resource"][player][thread_num] = {
          "maxrss": maxrss
        }
        

player_num  = sys.argv[1]
program     = sys.argv[2]
prof_data   = { "network": {}, "resource": {} }

mamba_proc = \
    subprocess.Popen(["./Player.x", player_num, program], \
        stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.PIPE)

in_queue = Queue()
mamba_thread = MambaOutThread(mamba_proc, in_queue, prof_data)

def user_input(varname, var):
  print "input", varname, ":",
  x = raw_input()
  arr = x.split()

  try:
    parsed_input = [int(val) for val in arr]
    if type(var) == int or type(var) == bool:
      if len(parsed_input) == 1:
        return parsed_input[0]

      else:
        raise ValueError("expecting single number as input")

    else:
      if len(var) == len(parsed_input):
        return parsed_input

      else:
        raise ValueError("expecting array of length " + str(len(var)) + " as input")
      

  except ValueError as err:
    print err
    print "please enter new valid input"
    return user_input(varname, var)


def mamba_input(val):
  mamba_proc.stdin.write(str(val) + "\n")


def mamba_output():
  val = in_queue.get()
  return val


def user_output(val):
  print "output:", val


def print_profile_info(data):
  for player, player_data in data["network"].iteritems():
    bytes_sent = 0
    bytes_received = 0

    for thread, thread_data in player_data.iteritems():
      bytes_sent += thread_data["sent"]
      bytes_received += thread_data["received"]

    print "player {}, bytes sent: {}, bytes received: {}".format(player, bytes_sent, bytes_received)

  for player, player_data in data["resource"].iteritems():
    maxrss = 0

    for thread, thread_data in player_data.iteritems():
      maxrss += thread_data["maxrss"]

    print "player {}, maxrss: {}".format(player, maxrss)


mamba_thread.start()

local_proc = os.path.join(program, "player_{}.py".format(player_num))
execfile(local_proc)

mamba_thread.join()

print_profile_info(prof_data)

