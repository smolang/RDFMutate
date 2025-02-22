#!/bin/bash

# optional argument: max heap size in GB

heapLimit=${1:-12} # no limit: 12GB
limit=-Xmx${heapLimit}G 

echo "evaluating input coverage. This can take several minutes. On our machine (Intel Core i7-1165G7) it took about 3h."

# build 
echo "build source code"
./gradlew build 

# TODO: uncomment following line
#java $limit -jar build/libs/OntoMutate-0.1.jar --el-graph --coverage-samples=500

java $limit -jar build/libs/OntoMutate-0.1.jar --suave-coverage-graph --coverage-samples=100 # should be 2h for 100 samples



# create plot
cd sut/reasoners/evaluation
pdflatex inputCoverageEL.tex 
cd ../../..

cd sut/suave/evaluation
pdflatex inputCoverageSuave.tex 
cd ../../..

mkdir -p results
cp sut/reasoners/evaluation/inputCoverage.pdf results/inputCoverage.pdf
