#!/bin/bash

# applies one mutation

logFile=geoReplication.log

currentFolder=$(pwd)

# build 
echo "build source code"
./gradlew build


# run mutation (with the arguments provided)
echo "generating mutants. This might take a few (<5) minutes."
java -jar build/libs/OntoMutate-0.1.jar --scen_geo


echo "start test runs of system"
echo "WARNING: this might take up a severe amout of time. On our machine (Intel Core i7-1165G7) it took about 100 hours."



cd sut/geo

# start run for first mask
./runTests.sh mutatedOnt/geoMutant0.csv "defaultScenario"

# start runs for second mask, for all scenarios
./runTests.sh mutatedOnt/geoMutant1.csv "defaultScenario"

./runAllScenarios.sh mutatedOnt/geoMutant1.csv

cd $currentFolder
