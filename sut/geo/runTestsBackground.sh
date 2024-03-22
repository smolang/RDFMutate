#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2023

# usage: ./runTestsBackground.sh TESTS-FILE 
# output will be put in file oracle_TEST-FILE_DATE.csv


nohup ./runTests.sh $1 >temp/temp.log 2>temp/temp.log </dev/null &
