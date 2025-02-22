#!/bin/bash

# optional argument: max heap size in GB

heapLimit=${1:-8} # no limit: 8GB
limit=-Xmx${heapLimit}G 

echo "evaluating input coverage using reduced sample size (50 and 5)"

# build 
echo "build source code"
./gradlew build 

# heap size: 16GB
java $limit -jar build/libs/OntoMutate-0.1.jar --el-graph --coverage-samples=50

java $limit -jar build/libs/OntoMutate-0.1.jar --suave-coverage-graph --coverage-samples=5


# create plot
cd sut/reasoners/evaluation
pdflatex inputCoverage.tex 
cd ../../..

mkdir -p results
cp sut/reasoners/evaluation/inputCoverage.pdf results/inputCoverage.pdf