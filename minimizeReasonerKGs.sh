# minimizes input KG, while oracle w.r.t. the tested reasoners remains the same
# usage: ./reasonerOracle.sh ONTOLOGY-FILE
# result of oracle will be output on terminal (pass, fail)
# pass: reasoners agree with each other
# fail: reasoners do not agree with each other

ontologyFile=$1

minimalOntologyFile="$ontologyFile.minimal.owl"

ontologyFileContainer=testOnt.owl
minimalOntologyFileContainer="$ontologyFileContainer.minimal.owl"


CONTAINER_NAME=reasonerContainer

timeLimit=${2:-600}   # time limit default = 10min
memLimit=${3:-8} # no limit: 8GB


running=1

result=$( docker ps -q -f name=$CONTAINER_NAME )

if ! [[ -n "$result" ]]; then
  # container is not running
  echo 'container is not running' 
  docker start $CONTAINER_NAME
  running=0
fi

sleep 1

# copy ontology to container
docker cp $ontologyFile $CONTAINER_NAME:/$ontologyFileContainer

echo "minimize ontology"
# delete old minimal file
docker exec $CONTAINER_NAME rm ../$minimalOntologyFileContainer
# minimize ontology
docker exec $CONTAINER_NAME ./minimize-ontology.sh ../$ontologyFileContainer $timeLimit $memLimit

# copy result from container
docker cp $CONTAINER_NAME:/$minimalOntologyFileContainer $minimalOntologyFile

echo "save minimal ontology to file $minimalOntologyFile"



# stop container if it was not running before this script was executed
if [[ $running == 0 ]] ; then
    echo 'stopping container'
    docker stop $CONTAINER_NAME
fi
