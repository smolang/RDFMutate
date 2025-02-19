#!/bin/bash

# optional argument: max heap size in GB

heapLimit=${1:-16} # no limit: 16GB
limit=-Xmx${heapLimit}G 

echo "evaluating input coverage. This can take several minutes. On our machine (Intel Core i7-1165G7) it took about 45 minutes."

# build 
echo "build source code"
./gradlew build 

# heap size: 16GB
java $limit -jar build/libs/OntoMutate-0.1.jar --el-graph --coverage-samples=500

# create plot
cd sut/reasoners/evaluation
pdflatex inputCoverage.tex 
cd ../../..