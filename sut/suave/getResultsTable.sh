#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# extracts most recent results csv file from docker 
# usage: ./getROSlog.sh TARGE-FILE 


CONTAINER_NAME=suaveContainer

target=$1



running=1

result=$( docker ps -q -f name=$CONTAINER_NAME )

if ! [[ -n "$result" ]]; then
  # container is not running
  echo 'container is not running' 
  docker start $CONTAINER_NAME
  running=0
fi

sleep 1

FileName=$(docker exec --workdir /home/kasm-user/suave/results $CONTAINER_NAME ls -Artp | tail -n 1)

echo $FileName

File=/home/kasm-user/suave/results/$FileName

docker cp $CONTAINER_NAME:$File $target

# stop container if it was not running before this script was executed
if [[ $running == 0 ]] ; then
    echo 'stopping container'
    docker stop $CONTAINER_NAME
fi