#!/bin/bash

# tests how well the minimization algorithm works

# read arguments
inputDirectory=$1  # e.g. testInputsToMinimize
timeLimit=${2:-1}   # timeLimit per minimization; default: 1 minutes


# time limit in second
timeLimitSeconds=$((60* $timeLimit))
echo "time limit is $timeLimitSeconds s"

# directory to store outputs
hostname=$(hostname)
outputDirectory=$inputDirectory/$hostname/output_$(date +'%Y_%m_%d_%H_%M')
echo "create directory $outputDirectory"
mkdir -p $outputDirectory

log=$outputDirectory/testMinimizer.log
outputCSV=$outputDirectory/overview.csv

echo "kg,noPossible,timeout,triplesInput,triplesMinimal,time(s)" > $outputCSV


CONTAINER_NAME=reasonerContainer
running=1
result=$( docker ps -q -f name=$CONTAINER_NAME )
if ! [[ -n "$result" ]]; then
  # container is not running
  echo 'container is not running' 
  docker start $CONTAINER_NAME
  running=0
fi

# build 
echo "build source code"
echo "build source code"  >> $log

cd ../../
./gradlew build 
cd sut/reasoners


# iterate over all files to calculate end time
expectedTime=0
for file in $inputDirectory/* ; do 
    if [ -f "$file" ]; then 
       # echo $file
       if [[ $file == "$inputDirectory"/*.owl && $file != "$inputDirectory"/*.minimal.owl ]]; then
            expectedTime=$(($expectedTime + $timeLimitSeconds))
       fi
    fi 
done

# calculate end time
endTime=$(date -d "$(date) + ${expectedTime} seconds")

# compute time limit
echo "maximal time: ${expectedTime} seconds" >> $log
echo "maximal end time: ${endTime}"
echo "maximal end time: ${endTime}" >> $log


# iterate over files for minimization
expectedTime=0
for file in $inputDirectory/* ; do 
    if [ -f "$file" ]; then 
       # minimiza all files that are not already minimal
       if [[ $file == "$inputDirectory"/*.owl && $file != "$inputDirectory"/*.minimal.owl ]]; then
            echo call ../../minimizeReasonerKGs.sh $file $timeLimitSeconds
            echo call ../../minimizeReasonerKGs.sh $file $timeLimitSeconds >> $log

            SECONDS=0   # variable to stop time
            ../../minimizeReasonerKGs.sh $file $timeLimitSeconds >> $log 2>&1
            duration=$SECONDS

            # count triples of old and minimal KG
            minimalFile="$file.minimal.owl"
            triplesOld=$(java -jar ../../build/libs/RDFMutate-0.1.jar --analyzeKG --seedKG=$file --owl) #>> $log #2>&1
            triplesOld=${triplesOld#"triples: "}

            timeout=0
            triplesNew=-1
            
            if [ -f "$minimalFile" ]; then 
                # minimalization worked
                triplesNew=$(java -jar ../../build/libs/RDFMutate-0.1.jar --analyzeKG --seedKG=$minimalFile --owl) #>> $log #2>&1
                triplesNew=${triplesNew#"triples: "}
            else 
                timeout=1
            fi
            echo "$file,0,$timeout,$triplesOld,$triplesNew,$duration" >> $outputCSV
            

       fi
    fi 
done

# add stuff that is not minimizable
for file in $inputDirectory/nonMinimizable/* ; do 
    if [ -f "$file" ]; then 
       # minimiza all files that are not already minimal
       if [[ $file == *.owl && $file != *.minimal.owl ]]; then
            triplesOld=-1
            triplesOld=$(java -jar ../../build/libs/RDFMutate-0.1.jar --analyzeKG --seedKG=$file --owl) #>> $log #2>&1
            triplesOld=${triplesOld#"triples: "}
            echo "$file,1,0,$triplesOld,-1,-1" >> $outputCSV
       fi
    fi
done


echo "finished minimizations"

# stop container if it was not running before this script was executed
if [[ $running == 0 ]] ; then
    echo 'stopping container'
    docker stop $CONTAINER_NAME
fi