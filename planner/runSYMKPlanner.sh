#!/bin/bash

# simple script that reads location of planner from config file and executes planner

# usage: ./runSYMKPlanner.sh DOMAIN PROBLEM PLAN

domain=$1
problem=$2
plan=$3

# directory of this file (to make coming calls possible, even if this script is called from somewhere else...)
Dir=$(dirname "$0")

# read config file to find planner
ConfigFile=$Dir/SYMKconfig.txt


# read config file to extract location of symk planner
# check if configuration file exists
if ! [[ -f "$ConfigFile" ]]; then
  echo "ERROR: No configuration file found. Please add configuration file $ConfigFile in this Folder."
  echo "exit script"
  exit 1
fi

foundFolder=0
while read line; do
	if [[ "$line" == folder=* ]]; then
		Folder=${line#*=} # remove everything left of and including "="
		foundFolder=1
	fi
done < "$ConfigFile"

if [[ $foundFolder == 0 ]]; then
  echo "ERROR: Configuration file $ConfigFile does not contain folder."
  echo "exit script"
  exit 1
fi

pathToPlanner=$Dir/$Folder/fast-downward.py


$pathToPlanner --plan-file $plan $domain $problem --search "sym-bd()"



