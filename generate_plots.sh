#!/bin/bash

# generates plot for evaluation

# optional argument: max heap size in GB

heapLimit=${1:-12} # no limit: 12GB
limit=-Xmx${heapLimit}G 

echo "evaluating input coverage. This can take several minutes. On our machine (Intel Core i7-1165G7) it took about 3h."

# build 
echo "build source code"
./gradlew build 

# create coverage plots


java $limit -jar build/libs/OntoMutate-0.1.jar --el-graph --coverage-samples=500

java $limit -jar build/libs/OntoMutate-0.1.jar --suave-coverage-graph --coverage-samples=100 # should be about 2h for 100 samples

cd sut/reasoners/evaluation
pdflatex inputCoverageEL.tex 
cd ../../..

cd sut/suave/evaluation
pdflatex inputCoverageSuave.tex 
cd ../../..

mkdir -p results
cp sut/reasoners/evaluation/inputCoverageEl.pdf results/inputCoverageEl.pdf
cp sut/suave/evaluation/inputCoverageSuave.pdf results/inputCoverageSuave.pdf

echo "evaluating attempts per mask. This can take several minutes. On our machine (Intel Core i7-1165G7) it took about 1h."


# create mask attempts plot
echo "create attempts graph"
java -jar build/libs/OntoMutate-0.1.jar --issre_graph --sample-size=100


cd sut/suave/evaluation
pdflatex plotAttemps.tex 
cd ../../..

mkdir -p results
cp sut/suave/evaluation/plotAttemps.pdf results/evaluation.pdf

