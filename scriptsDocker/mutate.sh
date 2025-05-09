#!/bin/sh

# applies one mutation

# run mutation (with the arguments provided)
echo "mutate KG"
java -jar build/libs/rdfmutate-1.0.jar --mutate "$@"
