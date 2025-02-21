#!/bin/bash

# generates issre graph

# build 
echo "build source code"
./gradlew build 

# run mutation (with the arguments provided)
echo "create attempts graph"
java -jar build/libs/OntoMutate-0.1.jar --issre_graph --sample-size=1


cd sut/suave/evaluation
pdflatex plotAttemps.tex 
cd ../../..

mkdir -p results
cp sut/suave/evaluation/plotAttemps.pdf results/evaluation.pdf

