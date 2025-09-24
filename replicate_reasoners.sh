#!/bin/bash

# reporduces reasoners evaluation from ESE journal paper

logFile=geoReplication.log

currentFolder=$(pwd)
timeLimit=${1:-600}   # time limit in minutes; default: 10h

# build 
echo "build source code"
./gradlew build



echo "start fuzzing reasoners"
echo "WARNING: this might take a severe amout of time, i.e. 20h."



cd sut/reasoners

# preform fuzzing
./fuzzReasoners.sh ontologies_ore $timeLimit
./fuzzReasonersLearnt.sh ontologies_ore $timeLimit


cd $currentFolder
