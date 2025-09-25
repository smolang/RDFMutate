#!/bin/bash

# reporduces suave evaluation from ISSRE paper
logFile=suaveReplication.log

currentFolder=$(pwd)

# build 
echo "build source code"
./gradlew build


# run mutation (with the arguments provided)
echo "generating mutants. This might take a few (<5) minutes."
java -jar build/libs/RDFMutate-0.1.jar --scen_suave


echo "start test runs of system"
echo "WARNING: this might take a severe amout of time. On our machine (Intel Core i7-1165G7) it took about 60 hours."



cd sut/suave

# start runs for all the generations
./runTests.sh mutatedOnt/generation0_domain_specific.csv 
./runTests.sh mutatedOnt/generation1_domain_specific.csv 
./runTests.sh mutatedOnt/generation2_domain_specific.csv 
./runTests.sh mutatedOnt/generation3_domain_independent.csv 
./runTests.sh mutatedOnt/generation4_domain_specific.csv 
./runTests.sh mutatedOnt/generation5_domain_specific.csv 
./runTests.sh mutatedOnt/generation6_domain_specific.csv 
./runTests.sh mutatedOnt/generation7_domain_specific.csv 



cd $currentFolder
