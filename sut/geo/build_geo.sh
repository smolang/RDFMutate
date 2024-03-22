#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./build_geo.sh 
# builds the geo simulator; has to be called only once


# read config file to find rewriter and planner
ConfigFile=config.txt

currentFolder=$(pwd)


# read config file to extract location of geo simulator
# check if configuration file exists
if ! [[ -f "$ConfigFile" ]]; then
  echo "ERROR: No configuration file found. Please add configuration file $ConfigFile in this Folder."
  echo "exit script"
  exit 1
fi

foundFolder=0
while read line; do
	if [[ "$line" == folder=* ]]; then
		Folder=${line#*=} # remove everything left of and including "="
		foundFolder=1
	fi
done < "$ConfigFile"

if [[ $foundFolder == 0 ]]; then
  echo "ERROR: Configuration file $ConfigFile does not contain folder."
  echo "exit script"
  exit 1
fi



# execute build script
cd $Folder
./gradlew shadowJar

# go back to folder with this script
cd $currentFolder
