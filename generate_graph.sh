#!/bin/bash

# applies one mutation

# build 
echo "build source code"
./gradlew build 

# run mutation (with the arguments provided)
echo "mutate KG"
java -jar build/libs/OntoMutate-0.1.jar --issre_graph


cd sut/suave/evaluation
pdflatex plotAttemps.tex 
cd ../../..

mkdir -p results
cp sut/suave/evaluation/plotAttemps.pdf results/evaluation.pdf

