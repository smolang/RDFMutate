# extracts most recent ROS log file from docker 
# usage: ./getROSlog.sh TARGE-FILE [HOW-OLD]

target=$1

CONTAINER_NAME=suaveContainer

# optional parameter: which log to retrieve: 
# 1: last log
# X: Xth-last log
number=${2:-1}

running=1

result=$( docker ps -q -f name=$CONTAINER_NAME )

if ! [[ -n "$result" ]]; then
  # container is not running
  echo 'container is not running' 
  docker start $CONTAINER_NAME
  running=0
fi

sleep 1

Folder=$(docker exec --workdir /home/kasm-user/.ros/log  $CONTAINER_NAME ls -Artp | grep /$ | tail -n $number | head -n 1)
echo "folder: $Folder"
LogFolder=/home/kasm-user/.ros/log/$Folder
echo "log_folder: $LogFolder"
LogFile=$(docker exec --workdir $LogFolder $CONTAINER_NAME ls -Art | tail -n 1)
LogFile=$LogFolder$LogFile
echo "log: $LogFile"

docker cp $CONTAINER_NAME:$LogFile $target

# stop container if it was not running before this script was executed
if [[ $running == 0 ]] ; then
    echo 'stopping container'
    docker stop $CONTAINER_NAME
fi
