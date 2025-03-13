#!/bin/sh

# applies one mutation

# run mutation (with the arguments provided)
echo "mutate KG"
java -jar build/libs/OntoMutate-0.1.jar --mutate "$@"
