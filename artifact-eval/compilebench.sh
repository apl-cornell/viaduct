#!/usr/bin/env bash

if [ $# -lt 2 ]
then
  printf "usage: ./compilebench.sh [GROUP] [OUTDIR]\n"
  exit
fi

VIADUCT_CMD="./viaduct"
BENCH_DIR="benchmarks"
ERASED_EXT="Erased"
EXT=".via"

BENCHMARKS=("Battleship" "BettingMillionaires" "Biomatch" "GuessingGame" "HhiScore" "HistoricalMillionaires" "Interval" "Kmeans" "Median" "Rochambeau" "TwoRoundBidding")
OUTDIR=$2

case $1 in
  "lan" )
    COST=""
    ERASED=false
    OUTEXT="Lan"
    ;;

  "wan" )
    COST="--wancost"
    ERASED=false
    OUTEXT="Wan"
    ;;

  "erased" )
    COST=""
    ERASED=true
    OUTEXT="Erased"
    ;;
esac

for BENCH in ${BENCHMARKS[@]};
do
  if [ "$ERASED" = false ]; then
    BENCH_FILE="$BENCH$EXT"
  else
    BENCH_FILE="$BENCH$ERASED_EXT$EXT"
  fi

  printf "compiling $BENCH_DIR/$BENCH_FILE\n"
  $VIADUCT_CMD -v compile "$BENCH_DIR/$BENCH_FILE" -o "$OUTDIR/$BENCH$OUTEXT$EXT" $COST
done

