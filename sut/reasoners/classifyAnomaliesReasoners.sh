#!/bin/bash

# sorts anomalies and owl files into folders according to the tool that is the outlier


# read arguments
inputDirectory=$1  # e.g. fuzzingResults/rdfuzz/[BENCHMARK-RUN]/sortedAnomalies/consistency
outputDirectory=$2  # e.g. fuzzingResults/rdfuzz/[BENCHMARK-RUN]/sortedAnomalies/consistency

elkDirectory=$outputDirectory/elk
hermitDirectory=$outputDirectory/hermit
openlletDirectory=$outputDirectory/openllet


mkdir -p $elkDirectory
mkdir -p $hermitDirectory
mkdir -p $openlletDirectory

# iterate over all files
for file in $inputDirectory/* ; do 
    if [ -f "$file" ]; then 
       # echo $file
       if [[ $file == $inputDirectory/anomaly.* ]] ; then
           
            # count occurences --> find outlier
            elk=0
            hermit=0
            openllet=0

            while read line; do
                # count reasoners
                if [[ $line == *"HERMIT"* ]] ; then
                    hermit=$((hermit+1))
                fi
                if [[ $line == *"OPENLLET"* ]] ; then
                    openllet=$((openllet+1))
                fi
                if [[ $line == *"ELK"* ]] ; then
                    elk=$((elk+1))
                fi


            done <$file

            # copy files to correct place

            if [[ $hermit -gt $elk ]] && [[ $hermit -gt $openllet ]]; then
                # Hermit is the outlier
                prefix=$inputDirectory/anomaly.
                suffix=.txt
                ontFile=${file#$prefix}
                ontFile=${ontFile%$suffix}
                anomFile=${file#$inputDirectory/}
                mv $file $hermitDirectory/$anomFile
                mv $inputDirectory/$ontFile $hermitDirectory/$ontFile
            fi

            if [[ $elk -gt $hermit ]] && [[ $elk -gt $openllet ]]; then
                # Hermit is the outlier
                prefix=$inputDirectory/anomaly.
                suffix=.txt
                ontFile=${file#$prefix}
                ontFile=${ontFile%$suffix}
                anomFile=${file#$inputDirectory/}
                mv $file $elkDirectory/$anomFile
                mv $inputDirectory/$ontFile $elkDirectory/$ontFile
            fi

            if [[ $openllet -gt $hermit ]] && [[ $openllet -gt $elk ]]; then
                # Hermit is the outlier
                prefix=$inputDirectory/anomaly.
                suffix=.txt
                ontFile=${file#$prefix}
                ontFile=${ontFile%$suffix}
                anomFile=${file#$inputDirectory/}
                mv $file $openlletDirectory/$anomFile
                mv $inputDirectory/$ontFile $openlletDirectory/$ontFile
            fi

            
       fi
    fi 
done
