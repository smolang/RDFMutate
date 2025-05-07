#!/bin/bash

# generates evaluation graphs

cd ..
# build 
#echo "build source code"
./gradlew build 

# generate evaluation data
echo "generate evaluation data"
java -jar build/libs/OntoMutate-0.1.jar --performance-test-limited

# produce graphs (requires pdflatex)
cd evaluation
pdflatex plotAttemps.tex 
pdflatex plotTimeMutationCount.tex 
pdflatex plotTimeSeedSize.tex 




