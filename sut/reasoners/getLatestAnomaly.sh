# gets the latest anomaly report from docker container
# usage: ./reasonerOracle.sh ONTOLOGY-FILE OUTPUT-FOLDER
# name of the ontology is used to name the ontology file
# the output folder is used to safe the report to

ontologyFile=$1
outputFolder=$2


CONTAINER_NAME=reasonerContainer

anomalyReportHost="$outputFolder/anomaly.$ontologyFile.txt"

timeLimit=600   # time limit = 10min


running=1

result=$( docker ps -q -f name=$CONTAINER_NAME )

if ! [[ -n "$result" ]]; then
  # container is not running
  echo 'container is not running' 
  docker start $CONTAINER_NAME
  running=0
fi

sleep 1

# get file name of latest anomaly file created
anomalyReportContainer=$(docker exec  $CONTAINER_NAME ls -Artp | grep anomaly | tail -n 1)

# copy anomaly file
docker cp $CONTAINER_NAME:/RDFuzz/$anomalyReportContainer $anomalyReportHost





# stop container if it was not running before this script was executed
if [[ $running == 0 ]] ; then
    echo 'stopping container'
    docker stop $CONTAINER_NAME
fi
