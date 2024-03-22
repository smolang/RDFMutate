#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2023

# usage: ./runTests.sh TESTS-FILE 
# output will be put in file oracle_TEST-FILE_DATE.csv

# TODO: add option to delete logs

TimeStart="$(date -u +%s)"


ontologies=$1
temp_oracle_output=temp/oracle_output_temp.txt


#hostname=$(hostname)
#directory="../benchmark_runs/${hostname}"
# create folder if it does not exist yet
#mkdir -p "$directory"
name=${1%".csv"}
name=$(echo "$name" | tr / _)
name="oracle_${name}_$(date +'%Y_%m_%d_%H_%M')"
result="${name}.csv"


echo "run tests from file $ontologies"
echo "start time: $(date +'%d.%m.%Y at %H:%M')"
echo "result will be written to $result"

read -r head < "$ontologies"
if [[ $head != 'id;folder;mutantFile;'* ]]; then
    echo "ERROR: tests file needs the following header:"
    echo "id,folder,ontology"
    echo "terminating bechmark script"
    exit 1
fi

echo "id,folder,ontology,oracle" > $result

while IFS=";" read -r id folder ontology rest
do
  # call oracle
  echo "call oracle for ontology $folder/$ontology on $(date +'%d.%m.%Y at %H:%M')"
  ./geo_oracle.sh "$folder/$ontology" > $temp_oracle_output

  # extract result and write to oracle file

  while read line; do
      if [[ "$line" == oracle:* ]]; then
          oracle_output=${line#*: } # remove everything left of and including ":
      fi
  done < "$temp_oracle_output"

    echo ${id},$folder,$ontology,$oracle_output >> $result
done < <(tail -n +2 "$ontologies")

TimeEnd="$(date -u +%s)"
Duration="$(bc <<<"$TimeEnd-$TimeStart")"

echo "total time to run tests: ${Duration}s"