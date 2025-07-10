#!/bin/bash

# parses all mappings to RDF graphs

cd mappings
for file in `ls *.yml` ; do
    name=${file%.yml}
    yarrrml-parser -i $name.yml -o $name.ttl
done 