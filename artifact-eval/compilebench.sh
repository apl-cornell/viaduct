#!/usr/bin/env bash

. scripts/viaduct_command.sh

if [ $# -lt 1 ]
then
  echo "usage: ./compilebench.sh [lan|wan|erased]"
  exit
fi

BENCH_DIR="benchmarks"
ERASED_EXT="Erased"
EXT=".via"

BENCHMARKS=("Battleship" "BettingMillionaires" "Biomatch" "GuessingGame" "HhiScore" "HistoricalMillionaires" "Interval" "Kmeans" "Median" "Rochambeau" "TwoRoundBidding")

GROUP=$1

case $GROUP in
  "lan" )
    COST=""
    ERASED=false
    ;;

  "wan" )
    COST="--wancost"
    ERASED=false
    ;;

  "erased" )
    COST=""
    ERASED=true
    ;;
esac

# Create build directory
OUTDIR=build/$GROUP
mkdir -p "$OUTDIR"
echo "Writing compiled programs to $OUTDIR"

for BENCH in "${BENCHMARKS[@]}"
do
  if [ "$ERASED" = false ]; then
    BENCH_FILE="$BENCH$EXT"
  else
    BENCH_FILE="$BENCH$ERASED_EXT$EXT"
  fi

  echo "compiling $BENCH_DIR/$BENCH_FILE"
  $VIADUCT_CMD -v compile "$BENCH_DIR/$BENCH_FILE" -o "$OUTDIR/$BENCH$EXT" $COST
done
