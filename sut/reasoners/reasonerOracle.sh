# tests, if the EL reasoners agree on the ontology
# usage: ./suave_oracle.sh ONTOLOGY-FILE
# result of oracle will be output on terminal (pass, fail)
# pass: reasoners agree with each other
# fail: reasoners do not agree with each other

ontologyFile=$1

ontologyFileContainer=testOnt.owl

CONTAINER_NAME=reasonerContainer


running=1

result=$( docker ps -q -f name=$CONTAINER_NAME )

if ! [[ -n "$result" ]]; then
  # container is not running
  echo 'container is not running' 
  docker start $CONTAINER_NAME
  running=0
fi

sleep 1

docker cp $ontologyFile $CONTAINER_NAME:/$ontologyFileContainer

docker exec $CONTAINER_NAME ./reasonerOracle.sh ../$ontologyFileContainer


# stop container if it was not running before this script was executed
if [[ $running == 0 ]] ; then
    echo 'stopping container'
    docker stop $CONTAINER_NAME
fi
