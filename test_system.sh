#!/bin/bash

# tests implementation, i.e. builds source code and executes some tasks
# 1. run "testMutation()" function


currentFolder=$(pwd)

logFile=test_system.log

# build 
echo "build source code"
./gradlew build

# run test (pipe inspection case)
echo "test test case generator"
java -jar build/libs/OntoMutate-0.1.jar --scen_test 

# run geo case
#echo "test geo simulator, this will take some minutes to complete"
#cd sut/geo
#./geo_oracle.sh total_mini.ttl >> $logFile
#cd $currentFolder

# run suave case
#echo "test suave simulator, this will take some (~10) minutes to complete"
#cd sut/suave

# add script to run suave only once here

#cd $currentFolder
