#!/bin/bash

# optional argument: max heap size in GB

heapLimit=${1:-8} # no limit: 16GB
limit=-Xmx${heapLimit}G 

echo "evaluating input coverage using reduced sample size (50)"

# build 
echo "build source code"
./gradlew build 

# heap size: 16GB
java $limit -jar build/libs/OntoMutate-0.1.jar --el-graph --coverage-samples=50

# create plot
cd sut/reasoners/evaluation
pdflatex inputCoverage.tex 
cd ../../..