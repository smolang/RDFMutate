#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./log_analyzer.sh LOG-FILE
# analysis a ROS-log from SUAVE to decide, if the run was successfull, or not
# i.e. acts as an oracle to detect correct behaviour
# result of analysis will be output on terminal (pass, fail, or exception)

LOG=$1

# extract different things from log
distance=-1
time=-1
exceptions=0

# test, if log file exists
if ! [[ -f "$LOG" ]]; then
    echo "ERROR: file $LOG does not exist."
    exit 1
fi


while read line; do
    if [[ $line == *"[mission_node]"* ]]; then
        if [[ $line == *"Time elapsed to detect pipeline:"* ]]; then
            # extract distance and only consider full length in meters
            time=${line#*Time elapsed to detect pipeline: }
            time=${time% seconds}
        fi 
        if [[ $line == *"Distance inspected:"* ]]; then
            # extract distance and only consider full length in meters
            distance=${line#*Distance inspected: }
            distance=${distance%.*}
        fi 
    fi
    if [[ $line == *"[adpatation_goal_bridge]: Exception:"* ]]; then
        exceptions=$(($exceptions + 1))
    fi
done < $LOG

if [[ $exceptions > 0 ]]; then
    echo "exception"
elif [[ $distance > 20 ]]; then
    echo "pass"
else
    echo "fail"
fi