#!/bin/bash

# applies one mutation

# build 
echo "build source code"
./gradlew build 

# run mutation (with the arguments provided)
echo "mutate KG"
java -jar build/libs/RDFMutate-0.1.jar --mutate "$@"
