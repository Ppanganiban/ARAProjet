#!/bin/bash

OUTPUT_JAR=ElectionLeader.jar
INPUT_CONF=LeaderElection/configs
MAX_SPEED=19
make clean

make all

if [ ! -e tests ]
then
  mkdir tests
fi

for i in '20' '40' '80' '100' '120'
do
  for j in `seq 1 6`
  do
    java -jar $OUTPUT_JAR $INPUT_CONF/configN$i\_V$MAX_SPEED\_0$j.txt >> tests/ouputN$i\_V$MAX_SPEED.txt 2>&1
    echo -e "\n***********************************\n" >> tests/ouputN$i\_V$MAX_SPEED.txt
  done
done
