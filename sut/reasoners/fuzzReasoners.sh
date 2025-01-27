#!/bin/bash

# generates mutants by applying mutations to exisitng ontologies
# ./fuzzReasoners.sh ONTOLOGY-FOLDER TIME-LIMIT[min]

# read arguments
inputDirectory=$1  # e.g. ontologies_ore
timeLimit=${2:-1}   # timeLimit in minutes; default: 1 minutes

# calculate end time
timeLimit=$(( timeLimit * 60 ))
endTime=$(date -d "$(date) + ${timeLimit} seconds")

# create necessary directories
hostname=$(hostname)
outputDirectory=fuzzingResults/$hostname/fuzzing_$(date +'%Y_%m_%d_%H_%M')
mkdir -p "$outputDirectory"

log=$outputDirectory/fuzzingCampaign.log

# compute time limit
echo "time limit: ${timeLimit} seconds" >> $log
echo "expected end time: ${endTime}" >> $log


# build 
echo "build source code"
echo "build source code"  >> $log

cd ../../
./gradlew build 
cd sut/reasoners


# start with actual mutation
numberMutations=5   # number of mutation operators that get applied

temp_oracle_output=$outputDirectory/oracle_output_temp.txt

CONTAINER_NAME=reasonerContainer
docker start $CONTAINER_NAME

count=0
anomalyCount=0

echo "start testing"

# do this as long as the time limit is not reached
while [ $SECONDS -lt $timeLimit ]; 
do
    # iterate over all ontology files that are used for mutation
    for ontology in $inputDirectory/*.owl ; do 
        # do not generate further ontologies, if end is already reached
        if [ $SECONDS -gt $timeLimit ]; then
            break
        fi
        if [ -f "$ontology" ]; then 
            # run mutation 
            count=$(( count + 1 ))

            echo "start new test ($count) at $(date) "
            echo "start new test ($count) at $(date) " >> $log


            mutantOntology=$outputDirectory/ont_$count.owl
            #echo "mutate KG (ontology) $ontology"
            echo "mutate KG (ontology) $ontology" >> $log
            #echo "mutant is saved as $mutantOntology"
            #echo "mutant is saved as $mutantOntology" >> $log


            java -jar ../../build/libs/OntoMutate-0.1.jar --el-mutate --seedKG=$ontology --num_mut=$numberMutations --selection_seed=$count --owl --overwrite --print-summary --out=$mutantOntology >> $log 2>&1
            # get oracle
            echo call reasoner oracle >> $log
            bash reasonerOracle.sh $mutantOntology > $temp_oracle_output

            pass=0
            fail=0
            undecided=0
            while read line; do
                if [[ "$line" == pass ]]; then
                    pass=1
                fi
                if [[ "$line" == fail ]]; then
                    fail=1
                fi
                if [[ "$line" == undecided ]]; then
                    undecided=1
                fi

                if [[ "$line" == timeout ]]; then
                    undecided=1
                fi
            done < "$temp_oracle_output"


            if [[ $fail == 1 ]]; then
                echo ANOMALY detected. Saved to $mutantOntology.
                echo found anomaly. Saved to $mutantOntology. >> $log
                anomalyCount=$(( anomalyCount + 1 ))
            fi

            if [[ $pass == 1 ]]; then
                echo no anomaly found >> $log
                # delete ontology --> safe disk space
                rm $mutantOntology
            fi

            if [[ $undecided == 1 ]]; then
                echo "no anomaly found (undecided)" >> $log
                # delete ontology --> safe disk space
                rm $mutantOntology
            fi

        fi 
    done

done

docker stop $CONTAINER_NAME

echo "Found anomalies in $anomalyCount cases out of $count test runs within $timeLimit seconds."
echo "Found anomalies in $anomalyCount cases out of $count test runs within $timeLimit seconds." >> $log
