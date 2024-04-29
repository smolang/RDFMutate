#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./runAllScenarios.sh TESTS-FILE
# runs the tests for all scenarios
# switches scenario in between runs (hardcoded at the moment)

tests=$1


for i in 0 1 2 3 4 5 6 7 8 9; do 
    echo switch to scenario $i; 
    ./switch_scenario.sh $i
    ./runTests.sh $tests scenario${i}
done
