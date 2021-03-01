#!/usr/bin/env bash

sudo -nv

# ethernet device; by default it is set to the loopback (localhost) device
ETH="lo"

# clear traffic rules
tc qdisc del dev $ETH root

case $2 in
  "lan" )
    tc qdisc add dev $ETH root handle 1: tbf rate 1000mbit 
    ;;
  "wan" )
    tc qdisc add dev $ETH root handle 1: tbf rate 100mbit burst 1mbit latency 400ms
    tc qdisc add dev $ETH parent 1:1 handle 10: netem delay 50ms
    ;;
esac
