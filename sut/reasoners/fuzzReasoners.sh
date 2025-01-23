#!/bin/bash

# generates mutants by applying mutations to exisitng ontologies
# ./fuzzReasoners.sh TIME-LIMIT[min]

timeLimit=${1:-1}   # timeLimit in minutes; default: 1 minutes
timeLimit=$(( timeLimit * 60 ))
endTime=$(date -d "$(date) + ${timeLimit} seconds")

log=temp/$(date +'%Y_%m_%d_%H_%M_%S').log
mkdir -p temp

echo "time limit: ${timeLimit} seconds" >> $log
echo "expected end time: ${endTime}" >> $log


# build 
echo "build source code"
echo "build source code"  >> $log

cd ../../
./gradlew build 
cd sut/reasoners

inputDirectory=ontologies_ore
outputDirectory=test_ontologies/temp
numberMutations=5   # number of mutation operators that get applied

temp_oracle_output=temp/oracle_output_temp.txt


count=1
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
            mutantOntology=$outputDirectory/ont_$count.owl
            echo "mutate KG (ontology) $ontology"
            echo "mutate KG (ontology) $ontology" >> $log

            java -jar ../../build/libs/OntoMutate-0.1.jar --el-mutate --seedKG=$ontology --num_mut=$numberMutations --selection_seed=$count --owl --overwrite --out=$mutantOntology >> $log 2>&1
            # get oracle
            echo call reasoner oracle >> $log
            bash reasonerOracle.sh $mutantOntology > $temp_oracle_output

            pass=0
            fail=0
            while read line; do
                if [[ "$line" == pass ]]; then
                    pass=1
                fi
                if [[ "$line" == fail ]]; then
                    fail=1
                fi
            done < "$temp_oracle_output"

            if [[ $fail == 1 ]]; then
                echo found anomaly. Saved to $mutantOntology.
                echo found anomaly. Saved to $mutantOntology. >> $log
                anomalyCount=$(( anomalyCount + 1 ))
            fi

            if [[ $pass == 1 ]]; then
                echo no anomaly found >> $log
            fi

            count=$(( count + 1 ))
        fi 
    done

done


echo "Found anomalies in $anomalyCount cases out of $count test runs within $timeLimit seconds."
echo "Found anomalies in $anomalyCount cases out of $count test runs within $timeLimit seconds." >> $log
