#!/bin/bash

# simple script that calls planner

# usage: ./runPlanner.sh DOMAIN PROBLEM PLAN

# preferred: absolute paths for the files!

domain=$1
problem=$2
plan=$3

# directory of this file (to make coming calls possible, even if this script is called from somewhere else...)
Dir=$(dirname "$0")

bash $Dir/runSYMKPlanner.sh $domain $problem $plan



