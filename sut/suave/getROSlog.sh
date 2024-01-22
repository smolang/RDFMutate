# extracts most recent ROS log file from docker 
# usage: ./getROSlog.sh TARGE-FILE [HOW-OLD]

target=$1

# optional parameter: which log to retrieve: 
# 1: last log
# X: Xth-last log
number=${2:-1}

docker start suaveContainer
sleep 1

Folder=$(docker exec --workdir /home/kasm-user/.ros/log  suaveContainer ls -Artp | grep /$ | tail -n $number | head -n 1)
echo "folder: $Folder"
LogFolder=/home/kasm-user/.ros/log/$Folder
echo "log_folder: $Folder"
LogFile=$(docker exec --workdir $LogFolder suaveContainer ls -Art | tail -n 1)
LogFile=$LogFolder$LogFile
echo "log: $LogFile"

docker cp suaveContainer:$LogFile $target

docker stop suaveContainer
