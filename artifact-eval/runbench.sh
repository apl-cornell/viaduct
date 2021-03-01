#!/usr/bin/env bash

if [ $# -lt 3 ]
then
  printf "usage: ./runbenchdist.sh [BENCH_DIR] [HOST] [TRIALS] [OUTFILE]\n"
  exit
fi

VIADUCT_CMD="./viaduct"
BENCH_DIR=$1
EXT=".via"

BENCHMARKS=("Kmeans" "Biomatch" "HhiScore" "HistoricalMillionaires" "Median" "TwoRoundBidding")
VARIANTS=("Bool" "Yao" "Lan" "Wan")
HOST=$2
TRIALS=$3
OUTFILE="$2-$4"

for (( i=1; i<=$TRIALS; i++ ))
do
  printf "starting trial $i\n" | tee -a "$OUTFILE"
  for BENCH in ${BENCHMARKS[@]};
  do
    for VARIANT in ${VARIANTS[@]};
    do
      printf "executing $BENCH_DIR/$BENCH-$VARIANT$EXT\n" | tee -a "$OUTFILE"
      $VIADUCT_CMD -v run $HOST "$BENCH_DIR/$BENCH$VARIANT$EXT" -in "$HOST-input.txt" 2>> "$OUTFILE"
      printf "\n" >> "$OUTFILE"
      sleep 5s
    done
  done
  printf "finished trial $i\n" | tee -a "$OUTFILE"
done
