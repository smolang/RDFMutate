#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./log_analyzer.sh LOG-FILE ORACLE
# analysis of output of geo-simulator
# i.e. acts as an oracle to detect correct behaviour
# ORACLE can be either 0 or 1 (depending on wether maturation occurs or not)
# result of analysis will be output on terminal (pass, fail, or exception)

LOG=$1
Oracle=$2

# extract different things from log
maturation=0


# test, if log file exists
if ! [[ -f "$LOG" ]]; then
    echo "ERROR: file $LOG does not exist."
    exit 1
fi



timeFlag=0
t="-200"

while read line; do
    if [[ $line == *"maturation on-going"* ]]; then
        # update end time as long as there is maturation
        maturation=1
    fi
done < $LOG

#echo $startMaturation
#echo $endMaturation
#echo $startTrap


if [[ $maturation == $Oracle ]]; then
    echo "pass"
else 
    echo "fail"
fi