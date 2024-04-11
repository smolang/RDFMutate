#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./switch_scenario.sh ID
# switches to a different scenario, i.e. updates config file 
# id refers to number in file scenarios/scenarios.csv

Scenarios="scenarios/scenarios.csv"
TempFile="config_temp.txt"
touch $TempFile
Config="config.txt"

ScenarioNumber=$1


# test, if scenarios file exists
if ! [[ -f "$Scenarios" ]]; then
    echo "ERROR: file $Scenarios does not exist."
    exit 1
fi

# test, if config file exists
if ! [[ -f "$Config" ]]; then
    echo "ERROR: file $Config does not exist."
    exit 1
fi

# copy path from old config
while read line; do
    if [[ $line == "folder="* ]]; then
        echo $TempFile
        echo $line > $TempFile
    fi
done < $Config


# extract scenario and oracle
while IFS="," read -r id scenario maturationOracle
do
  if [[ $id == $ScenarioNumber ]]; then
     echo "scenario=$scenario">> $TempFile
     echo "maturation=$maturationOracle" >> $TempFile
  fi
done < <(tail -n +2 "$Scenarios")

#echo $startMaturation
#echo $endMaturation
#echo $startTrap

mv $TempFile $Config