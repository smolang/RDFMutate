#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./csv_results_analyzer.sh CSV-FILE
# analysis a results-csv from SUAVE to decide, if the run was successfull, or not
# i.e. acts as an oracle to detect correct behaviour
# result of analysis will be output on terminal (pass, fail, or exception)

# CAVE: only consierds one (i.e. the last run) documented in the table

CSV=$1

# extract different things from log
distance=-1
time=-1

# test, if csv file exists
if ! [[ -f "$CSV" ]]; then
    echo "ERROR: file $CSV does not exist."
    exit 1
fi


while read line; do
    if [[ $line == *"metacontrol"* ]]; then
        #echo "metrics"
        # extract distance and only consider full length in meters
        time=${line#*,*,*,*,*,*,}
        time=${time%,*}
        #echo $time

        distance=${line#*,*,*,*,*,*,*,}
        distance=${distance%.*}
        #echo $distance
    fi

done < $CSV

if [[ $distance > 20 ]]; then
    echo "pass"
else
    echo "fail"
fi