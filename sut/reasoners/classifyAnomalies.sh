#!/bin/bash

# sorts anomalies and owl files into folders according to type of anomaly

# read arguments
inputDirectory=$1  # e.g. fuzzingResults/rdfuzz/[BENCHMARK-RUN]
outputDirectory=$2  # e.g. fuzzingResults/rdfuzz/[BENCHMARK-RUN]/sortedAnomalies

elDirectory=$outputDirectory/notEL
consistencyDirectory=$outputDirectory/consistency
classDirectory=$outputDirectory/classHierarchy
exceptionDirectory=$outputDirectory/exception


mkdir -p $elDirectory
mkdir -p $consistencyDirectory
mkdir -p $classDirectory
mkdir -p $exceptionDirectory

# iterate over all files
for file in $inputDirectory/* ; do 
    if [ -f "$file" ]; then 
       # echo $file
       if [[ $file == $inputDirectory/anomaly.* ]] ; then
            notEL=0     # not in EL
            consistency=0   # issue with consistency
            class=0         # issue with class hierarchy
            exception=0     # an exception occured


            while read line; do
                if [[ $line == *"Anomaly: not in EL"* ]] ; then
                    notEL=1
                fi
                if [[ $line == *"Class hierarchy differentiation"* ]] ; then
                    class=1
                fi
                if [[ $line == *"Consistency differentiation"* ]] ; then
                    consistency=1
                fi
                if [[ $line == *"exception thrown"* ]] ; then
                    exception=1
                fi
            done <$file

            # copy files to correct place

            if [[ $notEL == 1 ]] ; then
                prefix=$inputDirectory/anomaly.
                suffix=.txt
                ontFile=${file#$prefix}
                ontFile=${ontFile%$suffix}
                anomFile=${file#$inputDirectory/}
                mv $file $elDirectory/$anomFile
                mv $inputDirectory/$ontFile $elDirectory/$ontFile
            fi

            if [[ $consistency == 1 ]] ; then
                prefix=$inputDirectory/anomaly.
                suffix=.txt
                ontFile=${file#$prefix}
                ontFile=${ontFile%$suffix}
                anomFile=${file#$inputDirectory/}
                mv $file $consistencyDirectory/$anomFile
                mv $inputDirectory/$ontFile $consistencyDirectory/$ontFile
            fi

            if [[ $class == 1 ]] ; then
                prefix=$inputDirectory/anomaly.
                suffix=.txt
                ontFile=${file#$prefix}
                ontFile=${ontFile%$suffix}
                anomFile=${file#$inputDirectory/}
                mv $file $classDirectory/$anomFile
                mv $inputDirectory/$ontFile $classDirectory/$ontFile
            fi

            if [[ $exception == 1 ]] ; then
                prefix=$inputDirectory/anomaly.
                suffix=.txt
                ontFile=${file#$prefix}
                ontFile=${ontFile%$suffix}
                anomFile=${file#$inputDirectory/}
                mv $file $exceptionDirectory/$anomFile
                mv $inputDirectory/$ontFile $exceptionDirectory/$ontFile
            fi

            
       fi
    fi 
done
