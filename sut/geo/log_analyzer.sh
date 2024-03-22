#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./log_analyzer.sh LOG-FILE
# analysis of output of geo-simulator
# i.e. acts as an oracle to detect correct behaviour
# result of analysis will be output on terminal (pass, fail, or exception)

LOG=$1

# extract different things from log
startMaturation=10
endMaturation=10
startTrap=10


# test, if log file exists
if ! [[ -f "$LOG" ]]; then
    echo "ERROR: file $LOG does not exist."
    exit 1
fi



timeFlag=0
t="-200"

while read line; do
    if [[ $timeFlag == 1 ]]; then
        t=$line
        t=${t%.*}   # round to int
        timeFlag=0
    elif [[ $line == "\"t:\""* ]]; then
        timeFlag=1
    elif [[ $line == *"maturation on-going"* ]]; then
        # update end time as long as there is maturation
        endMaturation=$t
        if [[ $startMaturation == 10 ]]; then
            # first time that maturation occurs
            startMaturation=$t
        fi
    elif [[ $line == *"trap"* ]]; then
        if [[ $startTrap == 10 ]]; then
            startTrap=$t
        fi
    fi

    if [[ $line == *"[mission_metrics]"* ]]; then
      #  echo "metrics"
        if [[ $line == *"Time elapsed to detect pipeline:"* ]]; then
            # extract distance and only consider full length in meters
            time=${line#*Time elapsed to detect pipeline: }
            time=${time% seconds}
           # echo $time
        fi 
        if [[ $line == *"Distance inspected:"* ]]; then
            # extract distance and only consider full length in meters
            distance=${line#*Distance inspected: }
            distance=${distance%.*}
            #echo $distance
        fi 
    fi
    if [[ $line == *"[adpatation_goal_bridge]: Exception:"* ]]; then
        exceptions=$(($exceptions + 1))
    fi
done < $LOG

#echo $startMaturation
#echo $endMaturation
#echo $startTrap

if [[ $startMaturation -ne -52 ]]; then
    echo "fail"
elif [[ $endMaturation -ne -16 ]]; then
    echo "fail"
elif [[ $startTrap -ne -28 ]]; then
    echo "fail"
else
    echo "pass"
fi