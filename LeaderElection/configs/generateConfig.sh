#!/bin/bash

MAXSPEED=19
###### GENERATE CONFIG FILES #######

for i in '20' '40' '80' '100' '120'
do
  for j in `seq 1 6`
  do
    echo -e "N\t$i\nSEED\t$RANDOM\nMAXSPEED\t$MAXSPEED" > configN$i\_V$MAXSPEED\_0$j.txt
    cat config_default.txt >> configN$i\_V$MAXSPEED\_0$j.txt
  done
done



