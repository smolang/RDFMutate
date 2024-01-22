# extracts most recent ROS log file from docker 
# usage: ./getROSlog.sh TARGE-FILE [HOW-OLD]

target=$1

# optional parameter: which log to retrieve: 
# 1: last log
# X: Xth-last log
number=${2:-1}

running=1

result=$( docker ps -q -f name=$CONTAINER_NAME )

if ! [[ -n "$result" ]]; then
  # container is not running
  echo 'container is not running' 
  docker start suaveContainer
  running=0
fi

sleep 1

Folder=$(docker exec --workdir /home/kasm-user/.ros/log  suaveContainer ls -Artp | grep /$ | tail -n $number | head -n 1)
echo "folder: $Folder"
LogFolder=/home/kasm-user/.ros/log/$Folder
echo "log_folder: $LogFolder"
LogFile=$(docker exec --workdir $LogFolder suaveContainer ls -Art | tail -n 1)
LogFile=$LogFolder$LogFile
echo "log: $LogFile"

docker cp suaveContainer:$LogFile $target

# stop container if it was not running before this script was executed
if [[ $running == 0 ]] ; then
    echo 'stopping container'
    docker stop suaveContainer
fi
